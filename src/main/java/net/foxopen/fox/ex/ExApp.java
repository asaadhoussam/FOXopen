package net.foxopen.fox.ex;
import net.foxopen.fox.dom.DOM;

public class ExApp extends ExGeneral{
  static String TYPE = "Application Error";
  public ExApp(String msg)  {
    super(msg, TYPE, null, null);
  }
  public ExApp(String msg, DOM xml) {
    super(msg, TYPE, xml, null);
  }
  public ExApp(String msg, Throwable e) {
    super(msg, TYPE, null, e);
  }
  public ExApp(String msg, DOM xml, Throwable e) {
    super(msg, TYPE, xml, e);
  }
  public ExApp(String msg, String type, DOM xml, Throwable exception) {
    super(msg,type,xml,exception);
  }
}
 
