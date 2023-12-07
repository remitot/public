package com.technology.ibd.service.operationdata.service;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.Scanner;

/**
 * Spring-фильтр, позволяющий логировать тело запроса в методах контролера (штатными стредствами это сделть нельзя).
 * Более того, не все методы контроллера смогут корректно обработать запрос после этого фильтра (но многие смогут).
 * Тело запроса логируется в stdout (см. "///body=").
 */
@Component
@Order(1)
public class RequestBodyLoggingFilter implements Filter {

  public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
      super(request);
      InputStream requestInputStream = request.getInputStream();
      this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
    }
    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new CachedBodyServletInputStream(this.cachedBody);
    }
    @Override
    public BufferedReader getReader() throws IOException {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
      return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }
  }

  public static class CachedBodyServletInputStream extends ServletInputStream {
    private final InputStream cachedBodyInputStream;
    public CachedBodyServletInputStream(byte[] cachedBody) {
      this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
    }
    @Override
    public boolean isFinished() {
      try {
        return cachedBodyInputStream.available() == 0;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    @Override
    public boolean isReady() {
      return true;
    }
    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException();
    }
    @Override
    public int read() throws IOException {
      return cachedBodyInputStream.read();
    }
  }
  
  @Override
  public final void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    CachedBodyHttpServletRequest cbreq = new CachedBodyHttpServletRequest(req);
    
    String content = "";
    try (Scanner sc = new Scanner(new ByteArrayInputStream(cbreq.cachedBody))) {
      sc.useDelimiter("\\Z");
      if (sc.hasNext()) {
        content = sc.next();
      }
    }
    System.out.println("///body=[" + content + "]");
    
    chain.doFilter(cbreq, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
}