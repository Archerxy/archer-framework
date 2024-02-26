package demos;

import java.lang.annotation.Annotation;

import com.archer.framework.base.annotation.Inject;
import com.archer.framework.web.annotation.Filter;
import com.archer.framework.web.filter.AnnotationFilter;
import com.archer.framework.web.filter.FilterState;
import com.archer.log.Logger;
import com.archer.net.http.ContentType;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpStatus;

@Filter
public class TokenFilter implements AnnotationFilter {
	
	@Inject
	Logger log;
	
	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return Token.class;
	}

	@Override
	public FilterState inputMessage(HttpRequest req, HttpResponse res) {
		if(req.getHeader("access-token") == null) {
			
			log.info("access denied cause access-token is null");
			
			res.setStatus(HttpStatus.UNAUTHORIZED);
			res.setContentType(ContentType.APPLICATION_JSON);
			res.setContent("{\"msg\":\"token is required\"}".getBytes());
			
			return FilterState.END;
		}
		return FilterState.CONTINUE;
	}

	@Override
	public int priority() {
		return 0;
	}
	
}
