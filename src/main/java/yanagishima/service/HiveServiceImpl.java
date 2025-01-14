package yanagishima.service;

import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import me.geso.tinyorm.TinyORM;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.komamitsu.fluency.Fluency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yanagishima.config.YanagishimaConfig;
import yanagishima.exception.HiveQueryErrorException;
import yanagishima.pool.StatementPool;
import yanagishima.result.HiveQueryResult;
import yanagishima.util.QueryIdUtil;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static yanagishima.util.Constants.YANAGISHIAM_HIVE_JOB_PREFIX;
import static yanagishima.util.DbUtil.insertQueryHistory;
import static yanagishima.util.DbUtil.storeError;
import static yanagishima.util.FluentdUtil.buildStaticFluency;
import static yanagishima.util.PathUtil.getResultFilePath;
import static yanagishima.util.QueryEngine.hive;
import static yanagishima.util.QueryEngine.spark;
import static yanagishima.util.TimeoutUtil.checkTimeout;
import static yanagishima.util.TypeCoerceUtil.objectToString;

public class HiveServiceImpl implements HiveService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiveServiceImpl.class);

    private final YanagishimaConfig yanagishimaConfig;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final TinyORM db;
    private final Fluency fluency;
    private final StatementPool statementPool;

    @Inject
    public HiveServiceImpl(YanagishimaConfig yanagishimaConfig, TinyORM db, StatementPool statementPool) {
        this.yanagishimaConfig = yanagishimaConfig;
        this.db = db;
        this.fluency = buildStaticFluency(yanagishimaConfig);
        this.statementPool = statementPool;
    }

    @Override
    public String doQueryAsync(String engine, String datasource, String query, String userName, Optional<String> hiveUser, Optional<String> hivePassword) {
        String queryId = QueryIdUtil.generate(datasource, query, engine);
        executorService.submit(new Task(queryId, engine, datasource, query, userName, hiveUser, hivePassword));
        return queryId;
    }

    public class Task implements Runnable {
        private final String queryId;
        private final String engine;
        private final String datasource;
        private final String query;
        private final String userName;
        private final Optional<String> hiveUser;
        private final Optional<String> hivePassword;

        public Task(String queryId, String engine, String datasource, String query, String userName, Optional<String> hiveUser, Optional<String> hivePassword) {
            this.queryId = queryId;
            this.engine = engine;
            this.datasource = datasource;
            this.query = query;
            this.userName = userName;
            this.hiveUser = hiveUser;
            this.hivePassword = hivePassword;
        }

        @Override
        public void run() {
            try {
                int limit = yanagishimaConfig.getSelectLimit();
                getHiveQueryResult(queryId, engine, datasource, query, true, limit, userName, hiveUser, hivePassword, true);
            } catch (HiveQueryErrorException e) {
                LOGGER.warn(e.getCause().getMessage());
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public HiveQueryResult doQuery(String engine, String datasource, String query, String userName, Optional<String> hiveUser, Optional<String> hivePassword, boolean storeFlag, int limit) throws HiveQueryErrorException {
        String queryId = QueryIdUtil.generate(datasource, query, engine);
        return getHiveQueryResult(queryId, engine, datasource, query, storeFlag, limit, userName, hiveUser, hivePassword, false);
    }

    private HiveQueryResult getHiveQueryResult(String queryId, String engine, String datasource, String query, boolean storeFlag, int limit, String userName, Optional<String> hiveUser, Optional<String> hivePassword, boolean async) throws HiveQueryErrorException {
        checkDisallowedKeyword(userName, query, datasource, queryId, engine);
        checkSecretKeyword(userName, query, datasource, queryId, engine);
        checkRequiredCondition(userName, query, datasource, queryId, engine);

        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        String url = null;
        if (engine.equals(hive.name())) {
            url = yanagishimaConfig.getHiveJdbcUrl(datasource);
            if (yanagishimaConfig.isHiveImpersonation(datasource)) {
                url += ";hive.server2.proxy.user=" + userName;
            }
        } else if (engine.equals(spark.name())) {
            url = yanagishimaConfig.getSparkJdbcUrl(datasource);
        } else {
            throw new IllegalArgumentException(engine + " is illegal");
        }
        String user = yanagishimaConfig.getHiveJdbcUser(datasource);
        String password = yanagishimaConfig.getHiveJdbcPassword(datasource);

        if (hiveUser.isPresent() && hivePassword.isPresent()) {
            user = hiveUser.get();
            password = hivePassword.get();
        }

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            long start = System.currentTimeMillis();
            HiveQueryResult hiveQueryResult = new HiveQueryResult();
            hiveQueryResult.setQueryId(queryId);
            processData(engine, datasource, query, limit, userName, connection, queryId, start, hiveQueryResult, async);
            if (storeFlag) {
                insertQueryHistory(db, datasource, engine, query, userName, queryId, hiveQueryResult.getLineNumber());
            }
            emitExecutedEvent(userName, query, queryId, datasource, engine, System.currentTimeMillis() - start);
            return hiveQueryResult;

        } catch (SQLException e) {
            storeError(db, datasource, engine, queryId, query, userName, e.getMessage());
            throw new HiveQueryErrorException(queryId, e);
        }
    }

    private void processData(String engine, String datasource, String query, int limit, String userName, Connection connection, String queryId, long start, HiveQueryResult queryResult, boolean async) throws SQLException {
        Duration queryMaxRunTime = new Duration(yanagishimaConfig.getHiveQueryMaxRunTimeSeconds(datasource), TimeUnit.SECONDS);
        try (Statement statement = connection.createStatement()) {
            int timeout = (int) queryMaxRunTime.toMillis() / 1000;
            statement.setQueryTimeout(timeout);
            if (engine.equals(hive.name())) {
                String jobName = null;
                if (userName == null) {
                    jobName = YANAGISHIAM_HIVE_JOB_PREFIX + queryId;
                } else {
                    jobName = YANAGISHIAM_HIVE_JOB_PREFIX + userName + "-" + queryId;
                }
                statement.execute("set mapreduce.job.name=" + jobName);
                List<String> hiveSetupQueryList = yanagishimaConfig.getHiveSetupQueryList(datasource);
                for (String hiveSetupQuery : hiveSetupQueryList) {
                    statement.execute(hiveSetupQuery);
                }
            }

            if (async && yanagishimaConfig.isUseJdbcCancel(datasource)) {
                statementPool.putStatement(datasource, queryId, statement);
            }

            boolean hasResultSet = statement.execute(query);
            if (!hasResultSet) {
                try {
                    Path dst = getResultFilePath(datasource, queryId, false);
                    dst.toFile().createNewFile();
                    queryResult.setLineNumber(0);
                    queryResult.setRawDataSize(new DataSize(0, DataSize.Unit.BYTE));
                    queryResult.setRecords(new ArrayList<>());
                    queryResult.setColumns(new ArrayList<>());
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try (ResultSet resultSet = statement.getResultSet()) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                int columnCount = metadata.getColumnCount();
                List<String> columnNameList = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNameList.add(metadata.getColumnName(i));
                }

                Path dst = getResultFilePath(datasource, queryId, false);
                int lineNumber = 0;
                int maxResultFileByteSize = yanagishimaConfig.getHiveMaxResultFileByteSize();
                int resultBytes = 0;
                try (BufferedWriter bw = Files.newBufferedWriter(dst, StandardCharsets.UTF_8);
                     CSVPrinter printer = new CSVPrinter(bw, CSVFormat.EXCEL.withDelimiter('\t').withNullString("\\N").withRecordSeparator(System.getProperty("line.separator")))) {
                    printer.printRecord(columnNameList);
                    lineNumber++;
                    queryResult.setColumns(columnNameList);

                    List<List<String>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(objectToString(resultSet.getObject(i)));
                        }

                        printer.printRecord(row);
                        lineNumber++;
                        resultBytes += row.toString().getBytes(StandardCharsets.UTF_8).length;
                        if (resultBytes > maxResultFileByteSize) {
                            String message = format("Result file size exceeded %s bytes. queryId=%s, datasource=%s", maxResultFileByteSize, queryId, datasource);
                            storeError(db, datasource, engine, queryId, query, userName, message);
                            throw new RuntimeException(message);
                        }

                        if (query.toLowerCase().startsWith("show") || rows.size() < limit) {
                            rows.add(row);
                        } else {
                            queryResult.setWarningMessage(format("now fetch size is %d. This is more than %d. So, fetch operation stopped.", rows.size(), limit));
                        }

                        checkTimeout(db, queryMaxRunTime, start, datasource, engine, queryId, query, userName);
                    }
                    queryResult.setLineNumber(lineNumber);
                    queryResult.setRecords(rows);
                    if (async && yanagishimaConfig.isUseJdbcCancel(datasource)) {
                        statementPool.removeStatement(datasource, queryId);
                    }

                    DataSize rawDataSize = new DataSize(Files.size(dst), DataSize.Unit.BYTE);
                    queryResult.setRawDataSize(rawDataSize.convertToMostSuccinctDataSize());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void checkDisallowedKeyword(String userName, String query, String datasource, String queryId, String engine) {
        for (String keyword : yanagishimaConfig.getHiveDisallowedKeywords(datasource)) {
            if (query.trim().toLowerCase().startsWith(keyword)) {
                String message = format("query contains %s. This is the disallowed keywords in %s", keyword, datasource);
                storeError(db, datasource, engine, queryId, query, userName, message);
                throw new RuntimeException(message);
            }
        }
    }

    private void checkSecretKeyword(String userName, String query, String datasource, String queryId, String engine) {
        for (String keyword : yanagishimaConfig.getHiveSecretKeywords(datasource)) {
            if (query.contains(keyword)) {
                String message = "query error occurs";
                storeError(db, datasource, engine, queryId, query, userName, message);
                throw new RuntimeException(message);
            }
        }
    }

    private void checkRequiredCondition(String userName, String query, String datasource, String queryId, String engine) {
        for (String requiredCondition : yanagishimaConfig.getHiveMustSpecifyConditions(datasource)) {
            String[] conditions = requiredCondition.split(",");
            for (String condition : conditions) {
                String table = condition.split(":")[0];
                if (!query.startsWith("SHOW") && !query.startsWith("DESCRIBE") && query.contains(table)) {
                    String[] partitionKeys = condition.split(":")[1].split("\\|");
                    for (String partitionKey : partitionKeys) {
                        if (!query.contains(partitionKey)) {
                            String message = format("If you query %s, you must specify %s in where clause", table, partitionKey);
                            storeError(db, datasource, engine, queryId, query, userName, message);
                            throw new RuntimeException(message);
                        }
                    }
                }
            }
        }
    }

    private void emitExecutedEvent(String username, String query, String queryId, String datasource, String engine, long elapsedTime) {
        if (yanagishimaConfig.getFluentdExecutedTag().isEmpty()) {
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("elapsed_time_millseconds", elapsedTime);
        event.put("user", username);
        event.put("query", query);
        event.put("query_id", queryId);
        event.put("datasource", datasource);
        event.put("engine", engine);

        try {
            fluency.emit(yanagishimaConfig.getFluentdExecutedTag().get(), event);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
