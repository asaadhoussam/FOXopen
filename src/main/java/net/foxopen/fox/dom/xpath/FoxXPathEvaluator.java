/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.dom.xpath;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XPathWrapper;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.saxon.DynamicNamespaceContext;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;


/**
 * A FoxXPathEvalutor is used to determine how an XPath should be evaluated, execute it using the correct XPath engine,
 * and return the result. It is also responsible for any caching which takes place.
 * This version provides the default behaviour for FOX, but can be overloaded if required and delivered by the
 * {@link FoxXPathEvaluatorFactory}.<br><br>
 *
 * In this class, simple paths are evaluated using the FOX internal evaluator and
 * complex XPaths are delegated to Saxon. Compiled XPaths are cached for reuse.<br><br>
 *
 * This object is thread-safe and typically only one instantiation will be required.
 */
public class FoxXPathEvaluator {

  /**
   * Should XPaths be executed using XPath 1.0 compatibility mode?
   */
  private final boolean mUseXPathBackwardsCompatibility;

  static {
    EngineStatus.instance().registerStatusProvider(new XPathStatusProvider());
  }

  /**
   * Create a new FoxXPathEvaluator.
   * @param pUseXPathBackwardsCompatibility If true, the new evaluator will use XPath 1.0 compatibility mode.
   */
  FoxXPathEvaluator(boolean pUseXPathBackwardsCompatibility){
    mUseXPathBackwardsCompatibility = pUseXPathBackwardsCompatibility;
  }

  /**
   * Evaluates pXPath and returns the result of the evaluation. No :{context}s are supported in the XPath.
   * @param pXPath The XPath string to be evaluated.
   * @param pContextNode The initial context item of the XPath evaluation.
   * @return The result of the XPath evaluation.
   * @throws ExBadPath If the XPath syntax is invalid.
   */
  public XPathResult evaluate(String pXPath, DOM pContextNode)
  throws ExBadPath {
    return evaluate(pXPath, pContextNode, null, FoxXPathResultType.DOM_LIST);
  }

   /**
    * Evaluates pXPath and returns the result of the evaluation, using the default FoxXPathResultType (DOM List).
    * @param pXPath The XPath string to be evaluated.
    * @param pContextNode The initial context item of the XPath evaluation.
    * @param pContextUElem Optional ContextUElem for resolving :{context} references.
    * @return The result of the XPath evaluation.
    * @throws ExBadPath If the XPath syntax is invalid.
    * @see FoxXPathEvaluator#evaluate(String, DOM, ContextUElem, FoxXPathResultType)
    */
  public XPathResult evaluate(String pXPath, DOM pContextNode, ContextUElem pContextUElem)
  throws ExBadPath {
    return evaluate(pXPath, pContextNode, pContextUElem, FoxXPathResultType.DOM_LIST);
  }

  /**
   * Evaluates pXPath and returns the result of the evaluation. Internally, this may use Saxon or Fox's own simple
   * path evaluator, but this should be transparent to consumers.
   * @param pXPath The XPath string to be evaluated.
   * @param pContextNode The initial context item of the XPath evaluation.
   * @param pContextUElem Optional ContextUElem for resolving :{context} references.
   * @param pResultType The expected result type.
   * @return The result of the XPath evaluation.
   * @throws ExBadPath If the XPath syntax is invalid.
   */
  public XPathResult evaluate(String pXPath, DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType)
  throws ExBadPath {
    return evaluate(pXPath, pContextNode, pContextUElem, pResultType, XPathWrapper.NO_WRAPPER);
  }


  /**
   * Gets a FoxPath from the cache, or compiles one if it is not cached.
   * @param pPath The Path String.
   * @param pOptionalContextNode The Context node of the path expression. Used for determining if namespace aware processing
   * is required. Can be null.
   * @return The compiled or cached FoxPath.
   * @throws ExBadPath If the path String is invalid.
   */
  public FoxPath getOrCompilePath(String pPath, DOM pOptionalContextNode)
  throws ExBadPath {

    if(pPath == null){
      throw new ExInternal("pPath cannot be null.");
    }

    pPath = pPath.trim();

    if(pPath.length() == 0){
      throw new ExBadPath("pPath cannot be empty or all whitespace.");
    }

    //PN TODO benchmark performance of caching Simple Paths, or looking up in cache before doing constant/simple path check
    //(bear in mind this will skew cache miss ratios)
    FoxPath lPath;

    //First, see if this is a constant value (i.e. a number) - this method will return null if not
    lPath = FoxConstantPath.getFoxConstantPathOrNull(pPath);
    if(lPath != null){
      return lPath;
    }

    //Now, try this as a simple path - this method will return null if it's not simple
    lPath = FoxSimplePath.getFoxSimplePathOrNull(pPath);
    if(lPath != null){
      return lPath;
    }

    //Finally, must be a full XPath - look in the cache first
    lPath = (FoxPath) CacheManager.getCache(BuiltInCacheDefinition.FOX_XPATH_EVALUATORS).get(pPath);
    if (lPath == null){
      //Not a constant or simple path so invoke full Saxon XPath processing.
      //If the Context Document is namespace aware, we need to use a special XPath constructor to resolve the namespaces
      if(pOptionalContextNode != null && pOptionalContextNode.getDocControl().isNamespaceAware()){
        lPath = new FoxXPath(pPath, mUseXPathBackwardsCompatibility, new DynamicNamespaceContext(pOptionalContextNode.getRootElement()));
      }
      else {
        //The majority of XPaths will use the standard namespace resolver
        lPath = new FoxXPath(pPath, mUseXPathBackwardsCompatibility, null);
        //Cache the compiled XPath for future executions (only cache non-namespace aware XPaths to avoid potential namespace conflicts)
        CacheManager.getCache(BuiltInCacheDefinition.FOX_XPATH_EVALUATORS).put(pPath, (FoxXPath) lPath);
      }
    }

    return lPath;
  }

  /**
   * Evaluates pXPath and returns the result of the evaluation. Internally, this may use Saxon or Fox's own simple
   * path evaluator, but this should be transparent to consumers.
   * @param pXPath The XPath string to be evaluated.
   * @param pContextNode The initial context item of the XPath evaluation.
   * @param pContextUElem Optional ContextUElem for resolving :{context} references.
   * @param pResultType The expected result type.
   * @param pXPathWrapper The wrapping function which causes this XPath to be evaluated (i.e. string()). XPathWrapper.NO_WRAPPER
   * should be used to indicate that no wrapping function was used.
   * @return The result of the XPath evaluation.
   * @throws ExBadPath If the XPath syntax is invalid.
   */
  public XPathResult evaluate(String pXPath, DOM pContextNode, ContextUElem pContextUElem, FoxXPathResultType pResultType, XPathWrapper pXPathWrapper)
  throws ExBadPath {

    //Some validation
    if(pContextNode == null){
      throw new ExInternal("pContextNode cannot be null.");
    }

    //Shortcut self-expression to save time
    if(".".equals(pXPath)){
      return new XPathResult(pContextNode, FoxSimplePath.getFoxSimplePathOrNull("."));
    }

    FoxPath lXPath = getOrCompilePath(pXPath, pContextNode);

    //Execute and return result wrapper
    return lXPath.execute(pContextNode, pContextUElem, pResultType, pXPathWrapper);
  }

  /**
   * Outputs statistics for all the currently cached XPaths to an HTML string. Used for !XPATHINFO bang handler.
   * @param pOrderBy Optional string - either "time" for overall execution time or "usage" for number of executions.
   * If null or empty then the default is usage.
   * @return HTML string.
   */
//  public static String outputCacheInfoToHTMLString(String pOrderBy){
//    StringBuilder sb = new StringBuilder("<h1>FoxXPathEvaluator Cache Info</h1>");
//    sb.append(
//      "<table width=\"100%\">" +
//      "  <tr style=\"background-color:#CCC; font-weight:bold;\">" +
//      "    <td width=\"50%\">XPath</td>" +
//      "    <td>Compile Time</td>" +
//      "    <td>Exec Count</td>" +
//      "    <td>Avg Execution Time</td>" +
//      "    <td>Implicated Labels</td>" +
//      "    <td>Uses context node/doc?</td>" +
//      "  </tr>"
//    );
//
//    FoxCache<String, FoxXPath> lSortedFoxList = CacheManager.getFoxCache(CacheName.FOX_XPATH_EVALUATOR_CACHE);
//    List<FoxXPath> lSortedList = new ArrayList<FoxXPath>(lSortedFoxList.values());
//
//    boolean lOrderByUsage;
//    if("usage".equals(XFUtil.nvl(pOrderBy, "usage"))){
//      lOrderByUsage = true;
//    }
//    else if("time".equals(pOrderBy)){
//      lOrderByUsage = false;
//    }
//    else {
//      throw new ExInternal("Order by supports 'usage' or 'time' only.");
//    }
//
//    Collections.sort(
//      lSortedList
//    , lOrderByUsage ?
//        new Comparator<FoxXPath>(){
//          public int compare(FoxXPath xp1, FoxXPath xp2 ){
//            return xp2.getUsageCount() - xp1.getUsageCount();
//          }
//        }
//      : new Comparator<FoxXPath>(){
//          public int compare(FoxXPath xp1, FoxXPath xp2 ){
//            return (int) ((xp2.getCumulativExecTimeMS() / xp2.getUsageCount()) - (xp1.getCumulativExecTimeMS() / xp1.getUsageCount()));
//          }
//        }
//    );
//    int i = 0;
//
//    for(FoxXPath lXPath : lSortedList){
//      sb.append("<tr style=\"background-color:" + (i%2==0 ? "#DDD" : "#CCC") + "\">");
//      sb.append("<td>");
//      sb.append(lXPath.getOriginalPath());
//      sb.append("</td><td>");
//      sb.append(lXPath.getCompileTime());
//      sb.append("</td><td>");
//      sb.append(lXPath.getUsageCount());
//      sb.append("</td><td>");
//      if (lXPath.getUsageCount() > 0) {
//        sb.append(lXPath.getCumulativExecTimeMS() / lXPath.getUsageCount());
//      }
//      else {
//        sb.append(0);
//      }
//      sb.append("</td><td>");
//      Set<String> lLabelSet = lXPath.getLabelSet();
//      if(lLabelSet != null){
//        for(String lLabel : lLabelSet){
//          sb.append(lLabel + ", ");
//        }
//      } else {
//        sb.append("none found");
//      }
//      sb.append("</td><td>");
//      sb.append(lXPath.usesContextItemOrDocument());
//      sb.append("</td></tr>");
//      i++;
//    }
//
//    sb.append("</td></tr></table>");
//    return sb.toString();
//  }

}