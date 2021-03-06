package net.foxopen.fox.command.builtin;

import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.flow.XDoControlFlowError;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.assertion.AssertionFailsResult;

import java.util.Collection;
import java.util.Collections;

public class AssertFailsCommand
extends BuiltInCommand {

  private final XDoCommandList mNestedCommands;
  private final String mMessage;

  private AssertFailsCommand(Mod pMod, DOM pCommandElement)
  throws ExDoSyntax {
    super(pCommandElement);
    mMessage = pCommandElement.getAttr("message");
    mNestedCommands = XDoCommandList.parseNestedDoOrChildElements(pMod, pCommandElement);
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    XDoRunner lRunner = pRequestContext.createCommandRunner(false);
    boolean lFailed = false;
    String lErrorMsg = "";

    XDoControlFlow lCommandResult = XDoControlFlowContinue.instance();
    try {
      lCommandResult = lRunner.runCommands(pRequestContext, mNestedCommands);
    }
    catch(Throwable th) {
      lFailed = true;
      lErrorMsg = th.getMessage();
    }

    if(!lFailed && lCommandResult instanceof XDoControlFlowError) {
      lFailed = true;
      lErrorMsg = ((XDoControlFlowError) lCommandResult).getMessage();
    }

    AssertionFailsResult lAssertionResult = new AssertionFailsResult(mMessage, lErrorMsg, lFailed);

    //Behaviour after an assertion failure depends on the request's assertion mode
    return AssertCommand.handleAssertionResult(pRequestContext, lAssertionResult);
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new AssertFailsCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("assert-fails");
    }
  }
}
