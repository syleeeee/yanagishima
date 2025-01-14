package yanagishima.servlet;

import io.prestosql.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yanagishima.config.YanagishimaConfig;
import yanagishima.service.OldPrestoService;
import yanagishima.service.PrestoService;
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
public class PrestoAsyncServlet extends HttpServlet {

	private static Logger LOGGER = LoggerFactory.getLogger(PrestoAsyncServlet.class);

	private static final long serialVersionUID = 1L;

	private final OldPrestoService oldPrestoService;

	private final PrestoService prestoService;

	private final YanagishimaConfig yanagishimaConfig;

	@Inject
	public PrestoAsyncServlet(OldPrestoService oldPrestoService, PrestoService prestoService, YanagishimaConfig yanagishimaConfig) {
		this.oldPrestoService = oldPrestoService;
		this.prestoService = prestoService;
		this.yanagishimaConfig = yanagishimaConfig;
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		HashMap<String, Object> retVal = new HashMap<String, Object>();
		
		try {
			Optional<String> queryOptional = Optional.ofNullable(request.getParameter("query"));
			queryOptional.ifPresent(query -> {
				String userName = null;
				Optional<String> prestoUser = Optional.ofNullable(request.getParameter("user"));
				Optional<String> prestoPassword = Optional.ofNullable(request.getParameter("password"));
				if(yanagishimaConfig.isUseAuditHttpHeaderName()) {
					userName = request.getHeader(yanagishimaConfig.getAuditHttpHeaderName());
				} else {
					if (prestoUser.isPresent() && prestoPassword.isPresent()) {
						userName = prestoUser.get();
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
				if(userName != null) {
					LOGGER.info(String.format("%s executed %s in %s", userName, query, datasource));
				}
				if (prestoUser.isPresent() && prestoPassword.isPresent()) {
					if(prestoUser.get().length() == 0) {
						retVal.put("error", "user is empty");
						JsonUtil.writeJSON(response, retVal);
						return;
					}
				}
				try {
					String queryid;
					if(yanagishimaConfig.isUseOldPresto(datasource)) {
						queryid = oldPrestoService.doQueryAsync(datasource, query, userName, prestoUser, prestoPassword);
					} else {
						queryid = prestoService.doQueryAsync(datasource, query, userName, prestoUser, prestoPassword);
					}
					retVal.put("queryid", queryid);
				} catch (ClientException e) {
					if(prestoUser.isPresent()) {
						LOGGER.error(String.format("%s failed to be authenticated", prestoUser.get()));
					}
					LOGGER.error(e.getMessage(), e);
					retVal.put("error", e.getMessage());
				} catch (Throwable e) {
					LOGGER.error(e.getMessage(), e);
					retVal.put("error", e.getMessage());
				}
			});
		} catch (Throwable e) {
			LOGGER.error(e.getMessage(), e);
			retVal.put("error", e.getMessage());
		}

		JsonUtil.writeJSON(response, retVal);

	}

}
