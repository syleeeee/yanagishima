package yanagishima.servlet;

import me.geso.tinyorm.TinyORM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yanagishima.config.YanagishimaConfig;
import yanagishima.service.HiveService;
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
public class HiveAsyncServlet extends HttpServlet {

    private static Logger LOGGER = LoggerFactory
            .getLogger(HiveAsyncServlet.class);

    private static final long serialVersionUID = 1L;

    @Inject
    private TinyORM db;

    private YanagishimaConfig yanagishimaConfig;

    private final HiveService hiveService;

    @Inject
    public HiveAsyncServlet(YanagishimaConfig yanagishimaConfig, HiveService hiveService) {
        this.yanagishimaConfig = yanagishimaConfig;
        this.hiveService = hiveService;
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        HashMap<String, Object> retVal = new HashMap<String, Object>();

        Optional<String> queryOptional = Optional.ofNullable(request.getParameter("query"));
        queryOptional.ifPresent(query -> {
            try {
                String userName = null;
                Optional<String> hiveUser = Optional.ofNullable(request.getParameter("user"));
                Optional<String> hivePassword = Optional.ofNullable(request.getParameter("password"));
                if(yanagishimaConfig.isUseAuditHttpHeaderName()) {
                    userName = request.getHeader(yanagishimaConfig.getAuditHttpHeaderName());
                } else {
                    if (hiveUser.isPresent() && hivePassword.isPresent()) {
                        userName = hiveUser.get();
                    }
                }
                if (yanagishimaConfig.isUserRequired() && userName == null) {
                    sendForbiddenError(response);
                    return;
                }

                String datasource = getRequiredParameter(request, "datasource");
                if (yanagishimaConfig.isCheckDatasource() && !validateDatasource(request, datasource)) {
                    sendForbiddenError(response);
                    return;
                }
                String engine = getRequiredParameter(request, "engine");
                if (userName != null) {
                    LOGGER.info(String.format("%s executed %s in datasource=%s, engine=%s", userName, query, datasource, engine));
                }
                String queryid = hiveService.doQueryAsync(engine, datasource, query, userName, hiveUser, hivePassword);
                retVal.put("queryid", queryid);


            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
                retVal.put("error", e.getMessage());
            }
        });


        JsonUtil.writeJSON(response, retVal);

    }
}
