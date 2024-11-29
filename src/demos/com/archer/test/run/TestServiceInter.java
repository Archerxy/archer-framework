package com.archer.test.run;

import com.archer.test.run.TestService.MultipartVO;
import com.archer.test.run.TestService.RequestVO;
import com.archer.test.run.TestService.ResponseVO;

public interface TestServiceInter {

	public ResponseVO test(String id, String pathVar, RequestVO vo);
	
	public ResponseVO test2(String queryP, MultipartVO vo);
}
