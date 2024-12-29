import java.util.logging.Logger;

import com.catalyst.integ.CatalystIntegFunctionHandler;
import com.catalyst.integ.ZCIntegRequest;
import com.catalyst.integ.ZCIntegResponse;
import com.zc.cliq.util.ZCCliqUtil;

public class CanvaForCliq implements CatalystIntegFunctionHandler {
	Logger LOGGER = Logger.getLogger(CanvaForCliq.class.getName());

	@Override
	public ZCIntegResponse runner(ZCIntegRequest req) throws Exception {
		try {
			ZCIntegResponse resp =  ZCCliqUtil.executeHandler(req);
			return resp;
		} catch(Exception ex) {
			LOGGER.severe("Exception while executing handler.");
			throw ex;
		}
	}
}
