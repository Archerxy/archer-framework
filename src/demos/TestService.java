package demos;

import com.archer.framework.base.annotation.Inject;
import com.archer.framework.base.annotation.Service;
import com.archer.log.Logger;

@Service
public class TestService {
	
	@Inject
	Logger log;

	public ResponseVO test(String id, String pathVar, RequestVO vo) {
		
		log.info("get in service, id = {}", id);
		
		ResponseVO res = new ResponseVO();
		res.id = id;
		res.pathVar = pathVar;
		res.req = vo.req;
		return res;
	}
	public class RequestVO {
		String req;
	}
	public class ResponseVO {
		
		String id;
		
		String msg = "杜萌生";

		String pathVar;
		
		String req;
		
	}
}
