package com.gupao.servlet.framework.webmvc.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GpView {
   public final String DEFAUL_CONTENT_TYPE = "text/html;charset=utf-8";

   private File viewFile;

   public GpView(File viewFile) {
      this.viewFile = viewFile;
   }

   public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
      StringBuffer stringBuffer = new StringBuffer();
      try {
         RandomAccessFile randomAccessFile = new RandomAccessFile(this.viewFile, "r");
         String line = null;
         while (null != (line = randomAccessFile.readLine())) {
            line = new String(line.getBytes("ISO-8859-1"), "utf-8");
//            Pattern pattern = Pattern.compile("￥\\{[^\\}]+\\}", Pattern.CASE_INSENSITIVE);
            Pattern pattern = Pattern.compile("$\\{(.+?)\\}",Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
               String  paraName = matcher.group();
               paraName = paraName.replaceAll("￥\\{|\\}", "");
               Object paraValue = model.get(paraName);
               if (null == paraValue) {continue;}

               line = matcher.replaceAll(paraValue.toString());
               matcher = pattern.matcher(line);
            }
            stringBuffer.append(line);
         }
         response.setCharacterEncoding("utf-8");
         response.getWriter().write(stringBuffer.toString());
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
