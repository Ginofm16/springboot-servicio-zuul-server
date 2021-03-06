package com.formacionbdi.springboot.app.zuul.filters;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

@Component
public class PreTiempoTranscurridoFilter extends ZuulFilter{

	private static Logger log = LoggerFactory.getLogger(PreTiempoTranscurridoFilter.class);
	
	//para validar si se ejecuta o no el filtro
	@Override
	public boolean shouldFilter() {
		return true;
	}

	//aca se resuelve la logica del filtro
	@Override
	public Object run() throws ZuulException {

		//utilizaremos para pasar datos al request, para ello tenemos que obtener el objeto HttpRequest
		RequestContext ctx = RequestContext.getCurrentContext();
		//atraves de ctx obtenemos el request
		HttpServletRequest request = ctx.getRequest();
		
		log.info(String.format("%s request enrutado a %s", request.getMethod(), request.getRequestURL().toString()));
		
		//canculando el tiempo de inicio
		Long tiempoInicio = System.currentTimeMillis();
		request.setAttribute("tiempoInicio", tiempoInicio);
		
		return null;
	}

	//del tipo PRE, antes que se resuelva la ruta y antes de la comunicacion con el microservicio
	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 1;
	}

}
