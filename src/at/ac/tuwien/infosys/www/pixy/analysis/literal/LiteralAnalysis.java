package at.ac.tuwien.infosys.www.pixy.analysis.literal;

import at.ac.tuwien.infosys.www.pixy.analysis.AbstractLatticeElement;
import at.ac.tuwien.infosys.www.pixy.analysis.AbstractTransferFunction;
import at.ac.tuwien.infosys.www.pixy.analysis.GenericRepository;
import at.ac.tuwien.infosys.www.pixy.analysis.TransferFunctionId;
import at.ac.tuwien.infosys.www.pixy.analysis.alias.AliasAnalysis;
import at.ac.tuwien.infosys.www.pixy.analysis.interprocedural.AbstractAnalysisType;
import at.ac.tuwien.infosys.www.pixy.analysis.interprocedural.AbstractInterproceduralAnalysis;
import at.ac.tuwien.infosys.www.pixy.analysis.interprocedural.InterproceduralWorklist;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.AssignArray;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.AssignBinary;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.AssignReference;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.AssignSimple;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.AssignUnary;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.CallBuiltinFunction;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.CallPreparation;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.CallReturn;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.*;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.Define;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.Isset;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.Tester;
import at.ac.tuwien.infosys.www.pixy.analysis.literal.transferfunction.Unset;
import at.ac.tuwien.infosys.www.pixy.conversion.*;
import at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Nenad Jovanovic <enji@seclab.tuwien.ac.at>
 */
public class LiteralAnalysis extends AbstractInterproceduralAnalysis {
    private TacConverter tac;

    private GenericRepository<AbstractLatticeElement> repos;

    // preceding alias analysis (required for correct results)
    private AliasAnalysis aliasAnalysis;

    // list of Include's
    private List<Include> includeNodes;

//  ********************************************************************************
//  CONSTRUCTORS *******************************************************************
//  ********************************************************************************

//  LiteralAnalysis ****************************************************************

    public LiteralAnalysis(
        TacConverter tac,
        AliasAnalysis aliasAnalysis, AbstractAnalysisType analysisType,
        InterproceduralWorklist workList) {

        this.tac = tac;
        this.repos = new GenericRepository<>();
        this.aliasAnalysis = aliasAnalysis;
        this.includeNodes = new LinkedList<>();

        this.initGeneral(tac.getAllFunctions(), tac.getMainFunction(),
            analysisType, workList);
    }

    // dummy constructor
    public LiteralAnalysis() {
    }

//  initLattice ********************************************************************

    protected void initLattice() {
        this.lattice = new LiteralLattice(
            this.tac.getPlacesList(), this.tac.getConstantsTable(), this.functions,
            this.tac.getSuperglobalsSymbolTable());
        // start value for literal analysis:
        // a lattice element that adds no information to the default lattice element
        this.startValue = new LiteralLatticeElement();
        this.initialValue = this.lattice.getBottom();
    }

//  ********************************************************************************
//  TRANSFER FUNCTION GENERATORS ***************************************************
//  ********************************************************************************

    // returns a transfer function for an AssignSimple cfg node;
    // aliasInNode:
    // - if cfgNodeX is not inside a basic block: the same node
    // - else: the basic block
    protected AbstractTransferFunction assignSimple(AbstractCfgNode cfgNodeX, AbstractCfgNode aliasInNode) {

        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignSimple cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignSimple) cfgNodeX;
        Variable left = cfgNode.getLeft();
        Set<Variable> mustAliases = this.aliasAnalysis.getMustAliases(left, aliasInNode);
        Set<Variable> mayAliases = this.aliasAnalysis.getMayAliases(left, aliasInNode);

        return new AssignSimple(
            left,
            cfgNode.getRight(),
            mustAliases,
            mayAliases);
    }

    protected AbstractTransferFunction assignUnary(AbstractCfgNode cfgNodeX, AbstractCfgNode aliasInNode) {

        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignUnary cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignUnary) cfgNodeX;
        Variable left = cfgNode.getLeft();
        Set<Variable> mustAliases = this.aliasAnalysis.getMustAliases(left, aliasInNode);
        Set<Variable> mayAliases = this.aliasAnalysis.getMayAliases(left, aliasInNode);

        return new AssignUnary(
            left,
            cfgNode.getRight(),
            cfgNode.getOperator(),
            mustAliases,
            mayAliases);
    }

    protected AbstractTransferFunction assignBinary(AbstractCfgNode cfgNodeX, AbstractCfgNode aliasInNode) {

        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignBinary cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignBinary) cfgNodeX;
        Variable left = cfgNode.getLeft();
        Set<Variable> mustAliases = this.aliasAnalysis.getMustAliases(left, aliasInNode);
        Set<Variable> mayAliases = this.aliasAnalysis.getMayAliases(left, aliasInNode);

        return new AssignBinary(
            left,
            cfgNode.getLeftOperand(),
            cfgNode.getRightOperand(),
            cfgNode.getOperator(),
            mustAliases,
            mayAliases,
            cfgNode);
    }

    protected AbstractTransferFunction assignRef(AbstractCfgNode cfgNodeX) {
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignReference cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignReference) cfgNodeX;
        return new AssignReference(
            cfgNode.getLeft(),
            cfgNode.getRight());
    }

    protected AbstractTransferFunction unset(AbstractCfgNode cfgNodeX) {
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Unset cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Unset) cfgNodeX;
        return new Unset(cfgNode.getOperand());
    }

    protected AbstractTransferFunction assignArray(AbstractCfgNode cfgNodeX) {
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignArray cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.AssignArray) cfgNodeX;
        return new AssignArray(cfgNode.getLeft());
    }

    protected AbstractTransferFunction callPrep(AbstractCfgNode cfgNodeX, TacFunction traversedFunction) {

        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallPreparation cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallPreparation) cfgNodeX;
        TacFunction calledFunction = cfgNode.getCallee();
        TacFunction callingFunction = traversedFunction;

        // call to an unknown function;
        // should be prevented in practice (all functions should be
        // modeled in the builtin functions file), but if
        // it happens: assume that it doesn't do anything to
        // literal information
        if (calledFunction == null) {
            // how this works:
            // - propagate with ID transfer function to Call
            // - the analysis algorithm propagates from Call
            //   to CallReturn with ID transfer function
            // - we propagate from CallReturn to the following node
            //   with a special transfer function that only assigns the
            //   literal of the unknown function's return variable to
            //   the temporary catching the function's return value
            return TransferFunctionId.INSTANCE;
        }

        // extract actual and formal params
        List<TacActualParameter> actualParams = cfgNode.getParamList();
        List<TacFormalParameter> formalParams = calledFunction.getParams();

        // the transfer function to be assigned to this node
        AbstractTransferFunction tf = null;

        if (actualParams.size() > formalParams.size()) {
            // more actual than formal params; either a bug or a varargs
            // occurrence;
            // note that cfgNode.getFunctionNamePlace() returns a different
            // result than function.getName() if "function" is
            // the unknown function
            throw new RuntimeException(
                "More actual than formal params for function " +
                    cfgNode.getFunctionNamePlace().toString() + " in file " +
                    cfgNode.getFileName() + ", line " + cfgNode.getOriginalLineNumber());
        } else {
            tf = new CallPreparation(actualParams, formalParams,
                callingFunction, calledFunction, this, cfgNode);
        }

        return tf;
    }

    protected AbstractTransferFunction entry(TacFunction traversedFunction) {
        return new FunctionEntry(traversedFunction);
    }

    protected AbstractTransferFunction callRet(AbstractCfgNode cfgNodeX, TacFunction traversedFunction) {

        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallReturn cfgNodeRet = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallReturn) cfgNodeX;
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallPreparation cfgNodePrep = cfgNodeRet.getCallPrepNode();
        TacFunction callingFunction = traversedFunction;
        TacFunction calledFunction = cfgNodePrep.getCallee();

        //System.out.println("rcall: " + callingFunction.getName() + " <- " + calledFunction.getName());

        // call to an unknown function;
        // for explanations see above (handling CallPreparation)
        AbstractTransferFunction tf;
        if (calledFunction == null) {
            tf = new CallReturnUnknown(cfgNodeRet);
        } else {

            // quite powerful transfer function, does many things
            tf = new CallReturn(
                this.interproceduralAnalysisInformation.getAnalysisNode(cfgNodePrep),
                callingFunction,
                calledFunction,
                cfgNodePrep,
                cfgNodeRet,
                this.aliasAnalysis,
                this.lattice.getBottom());
        }

        return tf;
    }

    protected AbstractTransferFunction callBuiltin(AbstractCfgNode cfgNodeX, TacFunction traversedFunction) {
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallBuiltinFunction cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.CallBuiltinFunction) cfgNodeX;
        return new CallBuiltinFunction(cfgNode);
    }

    protected AbstractTransferFunction callUnknown(AbstractCfgNode cfgNodeX, TacFunction traversedFunction) {
        CallUnknownFunction cfgNode = (CallUnknownFunction) cfgNodeX;
        return new CallUnknown(cfgNode);
    }

    protected AbstractTransferFunction global(AbstractCfgNode cfgNodeX) {

        // "global <var>";
        // equivalent to: creating a reference to the variable in the main function with
        // the same name
        Global cfgNode = (Global) cfgNodeX;

        // operand variable of the "global" statement
        Variable globalOp = cfgNode.getOperand();

        // retrieve the variable from the main function with the same name
        TacFunction mainFunc = this.mainFunction;
        SymbolTable mainSymTab = mainFunc.getSymbolTable();
        Variable realGlobal = mainSymTab.getVariable(globalOp.getName());

        // trying to declare something global that doesn't occur in the main function?
        if (realGlobal == null) {
            // we must not simply ignore this case, since the corresponding
            // local's literal would remain "NULL" (the default value for locals);
            // => approximate by assigning TOP to the operand

            Set<Variable> mustAliases = this.aliasAnalysis.getMustAliases(globalOp, cfgNode);
            Set<Variable> mayAliases = this.aliasAnalysis.getMayAliases(globalOp, cfgNode);

            return new AssignSimple(
                globalOp,
                Literal.TOP,
                mustAliases,
                mayAliases);
        } else {
            return new AssignReference(globalOp, realGlobal);
        }
    }

    protected AbstractTransferFunction isset(AbstractCfgNode cfgNodeX) {

        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Isset cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Isset) cfgNodeX;
        return new Isset(
            cfgNode.getLeft(),
            cfgNode.getRight());
    }

    protected AbstractTransferFunction define(AbstractCfgNode cfgNodeX) {
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Define cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Define) cfgNodeX;
        return new Define(this.tac.getConstantsTable(), cfgNode);
    }

    protected AbstractTransferFunction tester(AbstractCfgNode cfgNodeX) {
        // special node that only occurs inside the builtin functions file
        at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Tester cfgNode = (at.ac.tuwien.infosys.www.pixy.conversion.cfgnodes.Tester) cfgNodeX;
        return new Tester(cfgNode);
    }

    protected AbstractTransferFunction include(AbstractCfgNode cfgNodeX) {
        this.includeNodes.add((Include) cfgNodeX);
        return TransferFunctionId.INSTANCE;
    }

//  ********************************************************************************
//  GET ****************************************************************************
//  ********************************************************************************

    // returns the (folded) literal of the given place coming in to the given node;
    // TOP if the cfgNode is unreachable (and hence can't return a folded value)
    public Literal getLiteral(AbstractTacPlace place, AbstractCfgNode cfgNode) {

        LiteralLatticeElement element =
            (LiteralLatticeElement) this.interproceduralAnalysisInformation.getAnalysisNode(cfgNode).getUnrecycledFoldedValue();

        if (element == null) {
            return Literal.TOP;
        } else {
            return element.getLiteral(place);
        }
    }

    public Literal getLiteral(String varName, AbstractCfgNode cfgNode) {
        Variable var = this.tac.getVariable(cfgNode.getEnclosingFunction(), varName);
        if (var == null) {
            // you gave me the name of a variable that does not exist
            return Literal.TOP;
        }
        return this.getLiteral(var, cfgNode);
    }

    public List<Include> getIncludeNodes() {
        return this.includeNodes;
    }

//  ********************************************************************************
//  OTHER **************************************************************************
//  ********************************************************************************

//  evalIf *************************************************************************

    // NOTE: messages about successful evaluation of an "if" expression which
    // obviously can't be evaluated statically is not necessarily an indication
    // of an analysis bug: it only means that under the current analysis state,
    // the condition clearly evaluates to a known boolean; further iterations
    // might change this
    protected Boolean evalIf(If ifNode, AbstractLatticeElement inValueX) {
        return null;
    }

    // evaluates the given if-condition using the folded incoming values
    // (don't call this before literal analysis hasn't finished its work)
    public Boolean evalIf(If ifNode) {

        // incoming value at if node (folded)
        LiteralLatticeElement folded =
            (LiteralLatticeElement) getAnalysisNode(ifNode).getUnrecycledFoldedValue();
        if (folded == null) {
            // this means that literal analysis never reaches this point;
            // throw new RuntimeException("SNH, line " + ifNode.getOriginalLineNumber());
            return null;
        }

        return this.evalIf(ifNode, folded);
    }

//  recycle ************************************************************************

    public AbstractLatticeElement recycle(AbstractLatticeElement recycleMe) {
        return this.repos.recycle(recycleMe);
    }

//  clean **************************************************************************

    // performs post-analysis cleanup operations to save memory
    public void clean() {
        // although we don't perform recycling during the analysis, we
        // do perform recycling for folding & cleaning; otherwise, cleaning
        // could result in bigger memory consumption than before
        this.interproceduralAnalysisInformation.foldRecycledAndClean(this);
    }
}