package yanagishima.servlet;

import me.geso.tinyorm.TinyORM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yanagishima.config.YanagishimaConfig;
import yanagishima.row.Query;
import yanagishima.util.HistoryUtil;
import yanagishima.util.JsonUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static yanagishima.util.AccessControlUtil.sendForbiddenError;
import static yanagishima.util.AccessControlUtil.validateDatasource;
import static yanagishima.util.HttpRequestUtil.getRequiredParameter;

@Singleton
public class HistoryServlet extends HttpServlet {

    private static Logger LOGGER = LoggerFactory
            .getLogger(HistoryServlet.class);

    private static final long serialVersionUID = 1L;

    @Inject
    private TinyORM db;

    private YanagishimaConfig yanagishimaConfig;

    @Inject
    public HistoryServlet(YanagishimaConfig yanagishimaConfig) {
        this.yanagishimaConfig = yanagishimaConfig;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        HashMap<String, Object> retVal = new HashMap<String, Object>();

        try {
            Optional<String> queryidOptional = Optional.ofNullable(request.getParameter("queryid"));
            if(queryidOptional.isPresent()) {
                String datasource = getRequiredParameter(request, "datasource");
                if (yanagishimaConfig.isCheckDatasource() && !validateDatasource(request, datasource)) {
                    sendForbiddenError(response);
                    return;
                }
                String engine = request.getParameter("engine");
                Optional<Query> queryOptional;
                if(engine == null) {
                    queryOptional = db.single(Query.class).where("query_id=? and datasource=?", queryidOptional.get(), datasource).execute();
                } else {
                    queryOptional = db.single(Query.class).where("query_id=? and datasource=? and engine=?", queryidOptional.get(), datasource, engine).execute();
                }

                String userName = request.getHeader(yanagishimaConfig.getAuditHttpHeaderName());
                Optional<Query> userQueryOptional = db.single(Query.class).where("query_id=? and datasource=? and user=?", queryidOptional.get(), datasource, userName).execute();
                if(userQueryOptional.isPresent()) {
                    retVal.put("editLabel", true);
                } else {
                    retVal.put("editLabel", false);
                }

                queryOptional.ifPresent(query -> {
                    retVal.put("engine", query.getEngine());
                    if(yanagishimaConfig.isAllowOtherReadResult(datasource)) {
                        HistoryUtil.createHistoryResult(retVal, yanagishimaConfig.getSelectLimit(), datasource, query, true);
                    } else {
                        if(userQueryOptional.isPresent()) {
                            HistoryUtil.createHistoryResult(retVal, yanagishimaConfig.getSelectLimit(), datasource, query, true);
                        } else {
                            HistoryUtil.createHistoryResult(retVal, yanagishimaConfig.getSelectLimit(), datasource, query, false);
                        }
                    }
                });
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            retVal.put("error", e.getMessage());
        }

        JsonUtil.writeJSON(response, retVal);

    }



}
