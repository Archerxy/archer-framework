package com.archer.test.run;

import com.archer.framework.base.annotation.Controller;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.web.annotation.PathParam;
import com.archer.framework.web.annotation.Post;
import com.archer.framework.web.annotation.QueryParam;
import com.archer.test.run.TestService.MultipartVO;
import com.archer.test.run.TestService.RequestVO;
import com.archer.test.run.TestService.ResponseVO;

@Controller(prefix = "test")
public class TestController {
	
	@Inject
	TestServiceInter service;
	
	@Post(pattern = "/{id}/req")
	public ResponseVO test(@PathParam(name = "id") String id, @QueryParam(name = "pathVar") String pathVar, RequestVO vo) {
		return service.test(id, pathVar, vo);
	}
	
	@Post(pattern = "/multipart")
	@Token
	public ResponseVO test2(@QueryParam(name = "queryP") String queryP, MultipartVO vo) {
		return service.test2(queryP, vo);
	}
}
