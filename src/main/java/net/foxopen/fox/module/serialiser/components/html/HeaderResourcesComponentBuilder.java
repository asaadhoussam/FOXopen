package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.logging.BoomerangBangHandler;
import net.foxopen.fox.logging.RequestLogger;
import net.foxopen.fox.module.CSSListItem;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.List;


public class HeaderResourcesComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new HeaderResourcesComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HeaderResourcesComponentBuilder() {
  }

  private static void javascript(HTMLSerialiser pSerialiser, String pPath) {
    pSerialiser.append("<script src=\"");
    pSerialiser.append(pPath);
    pSerialiser.append("\" type=\"text/javascript\"></script>");
  }

  private static void css(HTMLSerialiser pSerialiser, String pPath) {
    pSerialiser.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    pSerialiser.append(pPath);
    pSerialiser.append("\">");
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    if (pSerialiser.isHeaderResourcesIncluded()) {
      return;
    }

    //TODO PN logging should be based on flag on request, or at least on engine config
    if(RequestLogger.LOG_USER_EXPERIENCE_TIMES) {

      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/boomerang/boomerang.js"));
      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/boomerang/plugins/rt.js"));
      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/boomerang/plugins/navtiming.js"));

      pSerialiser.append("<script>\n" +
      "  BOOMR.init({\n" +
      "    beacon_url: '" + pSerialisationContext.createURIBuilder().buildBangHandlerURI(BoomerangBangHandler.instance()) + "',\n" +
      "    log: null\n" +
      "  });\n" +
      "  BOOMR.addVar('" + BoomerangBangHandler.REQUEST_ID_PARAM_NAME + "','" + pSerialisationContext.getRequestLogId() + "');\n" +
      "  BOOMR.subscribe('before_beacon', function(o) {\n" +
      "    DevToolbar.setUserExperienceTime(o.t_done)\n" +
      "  });\n" +
      "</script>");
    }

    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/jquery.js"));
    css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/jquery-ui.css"));

    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/json2.js"));
    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/html5shiv.js"));

    if (1==1) { // TODO - Need a good way to summarise when this is needed
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/tooltipster.js"));
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/tooltipster.css"));
    }

    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/fontspy.js"));

    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/jquery-timepicker.js"));

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.SEARCH_SELECTOR)) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/tagger.js"));
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/tagger.css"));
    }

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.HTML)) {
      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/tiny_mce/tinymce.min.js"));
    }

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.FILE)) {

      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/jquery_file_upload/js/vendor/jquery.ui.widget.js"));
      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/jquery_file_upload/js/jquery.iframe-transport.js"));
      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/jquery_file_upload/js/jquery.fileupload.js"));

      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/tiny_mce/tinymce.min.js"));

      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/fileUpload.js"));
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/fileUpload.css"));
    }

    //TODO PN serialiser implicated components (only include this if tabs have been served)
    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/tabs.js"));

    if(pSerialisationContext.getDownloadLinks().size() > 0) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/download"));
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/download"));
    }

    //TODO PN only if client visibility rules implicated
    CLIENT_VIS : {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/clientVisibility"));
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/clientVisibility"));
    }

    css(pSerialiser, pSerialisationContext.getContextResourceURI("/OpenSans/OpenSans.css"));
    css(pSerialiser, pSerialisationContext.getContextResourceURI("/icomoon/icomoon.css"));
    css(pSerialiser, pSerialisationContext.getContextResourceURI("/icomoon/icomoon-png.css"));

    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/autosize.js"));


    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/fox.js"));
    css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/fox.css"));

    // Include CSS from CSS List
    insertCSSItems(pSerialisationContext, pSerialiser);

    pSerialiser.setHeaderResourcesIncluded(true);
  }

  /**
   * Insert CSS links from CSS lists in modules and libraries
   * @param pSerialisationContext
   * @param pSerialiser
   */
  private void insertCSSItems(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser) {
    List<CSSListItem> lCSSItems = pSerialisationContext.getModule().getStyleSheets();

    for (CSSListItem lCSSItem : lCSSItems) {
      if (("accessible".equals(lCSSItem.getType()) && pSerialisationContext.isAccessibilityMode()) ||
          ("standard".equals(lCSSItem.getType()) && !pSerialisationContext.isAccessibilityMode()) ||
          "standard-and-accessible".equals(lCSSItem.getType())) {

        // TODO - NP - Dose of mustache needed
        String lLink = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + pSerialisationContext.getStaticResourceOrFixedURI(lCSSItem.getStyleSheetPath()) + "\">";

        String lBrowserCondition = lCSSItem.getBrowserCondition();
        if(!XFUtil.isNull(lBrowserCondition)) {
          lLink = "<!--[if "+lBrowserCondition+"]>"+lLink+"<![endif]-->";
        }

        pSerialiser.append(lLink);
      }
    }
  }
}