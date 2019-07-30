import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.awt.*;
import java.util.*;
import java.util.List;


class MyForwardFlow  extends ForwardFlowAnalysis<Unit, PointToSet>
{
	PointToSet inputSet = new PointToSet();
	PointToSet outputSet = new PointToSet();
	PointToSet returnSet = new PointToSet();
	DirectedGraph graph;
	Integer allocID = -1;
	int level = 0;
	int instNum = 0;
	TreeMap<Integer, ObjectInfo> queries = new TreeMap<Integer, ObjectInfo>();
	String methodName;
	String className;
	Set<String> callStack = new HashSet<String>();

	MyForwardFlow(DirectedGraph graph, PointToSet in, String name, String cName, Integer id, int l) {
		super(graph);
		this.graph = graph;
		this.methodName = name;
		this.className = cName;
		this.allocID = id;
		this.level = l;
		in.copy(this.inputSet);
		if (callStack.contains(name) == false) {
			callStack.add(name);
		}
		doAnalysis();
	}

	TreeMap<Integer, ObjectInfo> getQueries() {
		return queries;
	}

	void mergeQueries(TreeMap<Integer, ObjectInfo> newQueries) {
		// for (Map.Entry<Integer, ObjectInfo> q : newQueries.entrySet()) {
		// 	this.queries.putAll(newQueries);
		// }
		this.queries.putAll(newQueries);
	}


	String generateAnswer() {
		String answer = "";
		for (Map.Entry<Integer, ObjectInfo> q : queries.entrySet()) {
			Set<ObjectInfo> resultSet = outputSet.getKey(q.getValue());
			//TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			if (resultSet.isEmpty()) {
				answer += " \n";
				continue;
			}

			Iterator entries = resultSet.iterator();
			while(entries.hasNext()){
				ObjectInfo value = (ObjectInfo)entries.next();
				answer += " " + value.allocID;
			}
			answer += "\n";
		}
		System.out.println(answer);
		return answer;
	}

	static void dumpQueries(TreeMap<Integer, ObjectInfo> q) {
		for (Map.Entry<Integer, ObjectInfo> entry : q.entrySet()) {
			System.out.println("ID: " + entry.getKey());
			entry.getValue().dumpObjectInfo();
		}
	}

	public void showUnit(UnitGraph graph) {
		Map<String, Set<String>> r;
		Iterator unitIt = graph.iterator();
		while (unitIt.hasNext()) {
			System.out.println(unitIt);
		}
	}

	/**
	 * All INs are initialized to the empty set.
	 **/
	protected PointToSet newInitialFlow() {
		return inputSet.deepClone();
	}

	/**
	 * IN(Start) is the empty set
	 *
	 * @return*/
	protected PointToSet entryInitialFlow() {
		return inputSet.deepClone();
	}


	Set<ObjectInfo> handleInvoke(InvokeExpr ie, PointToSet in) {
		SootMethod sm = ie.getMethod();
		if (sm.getDeclaringClass().isJavaLibraryClass()) {
			return new HashSet<ObjectInfo>();
		}
		PointToSet initSet = new PointToSet();
		in.funcCallCopy(initSet);

		List<Value> args = ie.getArgs();
		for (int i = 0; i < args.size(); i++) {
			Value arg = args.get(i);
			if (arg instanceof Local) {
				System.out.println("Arg Name is " + arg.toString());
				ObjectInfo argObj = new ObjectInfo(0, arg.toString(), level, 2);
				ObjectInfo formalArgObj = new ObjectInfo(0, Integer.toString(i), level+1+instNum, -2);
				initSet.insert(formalArgObj, in.getKey(argObj));
			}
		}

		Set<ObjectInfo> retSet = new HashSet<ObjectInfo>();
		DirectedGraph graph = new ExceptionalUnitGraph(sm.retrieveActiveBody());
		if (ie instanceof SpecialInvokeExpr) {
			String baseName = ((Local) ((SpecialInvokeExpr)ie).getBase()).getName();
			System.out.println("Base name is " + baseName);
			ObjectInfo baseObj = new ObjectInfo(0, baseName, level, 2);

			Set<ObjectInfo> basePointSet = in.getKey(baseObj);


			PointToSet outSet = new PointToSet();
			Iterator entries = basePointSet.iterator();
			while(entries.hasNext()) {
				ObjectInfo value = (ObjectInfo)entries.next();
				Set<ObjectInfo> thisPointSet = new HashSet<ObjectInfo>();
				thisPointSet.add(value);
				ObjectInfo thisObj = new ObjectInfo(0, "this", level+1+instNum, -1);
				initSet.insert(thisObj, thisPointSet);
				MyForwardFlow flowAnalysis = new MyForwardFlow(graph, initSet, sm.getName(), sm.getDeclaringClass().toString(), value.getAllocID(), level+1+instNum);
				PointToSet tmpSet = new PointToSet();
				Set<ObjectInfo> tmpRetSet = flowAnalysis.returnSet.funcRetCopy(tmpSet);
				retSet.addAll(tmpRetSet);
				PointToSet tmpOutSet = new PointToSet();
				tmpSet.merge(outSet, tmpOutSet);
				tmpOutSet.copy(outSet);
				mergeQueries(flowAnalysis.queries);
			}
			outSet.funcRetCopy(in);
		} else {
			MyForwardFlow flowAnalysis = new MyForwardFlow(graph, initSet, sm.getName(), sm.getDeclaringClass().toString(), -1, level+1+instNum);
			retSet = flowAnalysis.returnSet.funcRetCopy(in);
			mergeQueries(flowAnalysis.queries);
			flowAnalysis.returnSet.dumpPointToSet();
		}
		// System.out.println("Out Set dump result is \n");
		// in.dumpPointToSet();

		return retSet;
	}



	protected void flowThrough(PointToSet inValue, Unit unit, PointToSet outValue) {
		PointToSet
				in = (PointToSet) inValue,
				out = (PointToSet) outValue;
		in.copy(out);
		Unit u = (Unit) unit;
		instNum = instNum + 100;

		System.out.println("[" + className + ":" + methodName + "]: " + unit);
		if (u instanceof InvokeStmt) {
			InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
			if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void alloc(int)>")) {
				// System.out.println("Invoke*****alloc\n");
				allocID = ((IntConstant)ie.getArgs().get(0)).value;
				//System.out.println("allocID = " + allocID);
			} else if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>")) {
				//System.out.println("Invoke*****test\n");
				Value v = ie.getArgs().get(1);
				int id = ((IntConstant)ie.getArgs().get(0)).value;
				ObjectInfo o = new ObjectInfo(0, v.toString(), level, 2);
				queries.put(id, o);
				//System.out.println("test invoke" + ": " + id + "," + v.toString());
			} else {
				if (callStack.contains(((InvokeStmt) u).getInvokeExpr().getMethod().toString()) == false) {
					callStack.add(((InvokeStmt) u).getInvokeExpr().getMethod().toString());
					handleInvoke(((InvokeStmt) u).getInvokeExpr(), out);
					callStack.remove(((InvokeStmt) u).getInvokeExpr().getMethod().toString());
				}
			}
		}
		if (u instanceof DefinitionStmt) {
			DefinitionStmt dsu = (DefinitionStmt)u;
			Value rightOp = dsu.getRightOp();
			Value leftOp = dsu.getLeftOp();

			Set<ObjectInfo> pointerSet = new HashSet<ObjectInfo>();
			Set<ObjectInfo> valueSet = new HashSet<ObjectInfo>();

			if (rightOp instanceof  NewExpr || rightOp instanceof NewArrayExpr) {
				//System.out.println("Right NewExpr**");
				ObjectInfo rightObj = new ObjectInfo(allocID, rightOp.toString(), level, 1);
				valueSet.add(rightObj);
			} else if (rightOp instanceof Local) {
				//System.out.println("Right Local**");
				ObjectInfo rightObj = new ObjectInfo(0, rightOp.toString(), level, 2);
				valueSet = out.getKey(rightObj);
			} else if (rightOp instanceof InstanceFieldRef) {
				//System.out.println("Right InstanceFieldRef**");
				InstanceFieldRef instsRefOp = (InstanceFieldRef) rightOp;
				ObjectInfo baseObj = new ObjectInfo(0, instsRefOp.getBase().toString(), level, 2);
				Set<ObjectInfo> basePointSet = out.getKey(baseObj);

				Iterator entries = basePointSet.iterator();
				while(entries.hasNext()){
					ObjectInfo value = (ObjectInfo)entries.next();
					String objName = value.objectName + "." + instsRefOp.getField().toString();
					System.out.println(objName);
					ObjectInfo fieldObj = new ObjectInfo(value.getAllocID(), objName, value.level, 1);
					Set<ObjectInfo> fieldPointSet = out.getKey(fieldObj);
					valueSet.addAll(PointToSet.deepCloneSet(fieldPointSet));  // Deep clone?
				}
			} else if (rightOp instanceof ThisRef) {
				//System.out.println("Right ThisRef**");
				ObjectInfo rightObj = new ObjectInfo(0, "this", level, -1);
				valueSet = out.getKey(rightObj);
			} else if (rightOp instanceof ParameterRef) {
				//System.out.println("Right ParameterRef**\n");
				String rightName = Integer.toString(((ParameterRef)rightOp).getIndex());
				ObjectInfo rightObj = new ObjectInfo(0, rightName, level, -2);
				valueSet = out.getKey(rightObj);
			}  else if (rightOp instanceof InvokeExpr) {
				//System.out.println("Right InvokeExpr**\n");
				if (callStack.contains(((InvokeStmt) u).getInvokeExpr().getMethod().toString()) == false) {
					callStack.add(((InvokeStmt) u).getInvokeExpr().getMethod().toString());
					valueSet = handleInvoke((InvokeExpr)rightOp, out);
					callStack.remove(((InvokeStmt) u).getInvokeExpr().getMethod().toString());
				}
			} else if (rightOp instanceof ArrayRef) {
				System.out.println("DEBUG: ArrayRef called");

				ArrayRef arr = (ArrayRef) rightOp;
				String baseName = ((Local) arr.getBase()).getName();
				String fieldName = "_";
				if (arr.getIndex() instanceof IntConstant) {
					fieldName = Integer.toString(((IntConstant) arr.getIndex()).value);
				}

				ObjectInfo rightObj = new ObjectInfo(0, baseName, level, 2);
				Set<ObjectInfo> basePointsTo = out.getKey(rightObj);

				for (ObjectInfo pto: basePointsTo) {
					ObjectInfo k1 = new ObjectInfo(pto.getAllocID(), baseName+fieldName, level, 2);
					valueSet.addAll(out.getKey(k1));
					if (!fieldName.equals("_")) {
						//固定位置
						ObjectInfo k2 = new ObjectInfo(pto.getAllocID(), baseName+fieldName, level, 2);
						valueSet.addAll(out.getKey(k2));
					}
				}
			}

			if (leftOp instanceof Local) {
				//System.out.println("Left Local**\n");
				ObjectInfo leftObj = new ObjectInfo(0, leftOp.toString(), level, 2);
				pointerSet.add(leftObj);
			} else if (leftOp instanceof InstanceFieldRef) {
				//System.out.println("Left InstanceFieldRef**\n");
				InstanceFieldRef instsRefOp = (InstanceFieldRef) leftOp;
				// System.out.println(instsRefOp.getField().toString() + "***" + instsRefOp.getBase().toString());
				ObjectInfo baseObj = new ObjectInfo(0, instsRefOp.getBase().toString(), level, 2);
				Set<ObjectInfo> basePointSet = out.getKey(baseObj);

				Iterator entries = basePointSet.iterator();
				while(entries.hasNext()) {
					ObjectInfo value = (ObjectInfo)entries.next();
					String objName = value.objectName + "." + instsRefOp.getField().toString();
					ObjectInfo fieldObj = new ObjectInfo(value.getAllocID(), objName, value.level, 1);
					System.out.println("fieldObj " + level);
					fieldObj.dumpObjectInfo();
					pointerSet.add(fieldObj.deepClone());
				}
			} else if (leftOp instanceof ArrayRef) {
				ArrayRef arr = (ArrayRef) leftOp;
				String baseName = ((Local) arr.getBase()).getName();
				String fieldName = "_";
				if (arr.getIndex() instanceof IntConstant) {
					fieldName = Integer.toString(((IntConstant) arr.getIndex()).value);
				}

				ObjectInfo leftIFObj = new ObjectInfo(0, baseName, level, 2);
				Set<ObjectInfo> basePointsTo = in.getKey(leftIFObj);

				for (ObjectInfo pto: basePointsTo) {
					ObjectInfo k1 = new ObjectInfo(pto.getAllocID(), baseName+fieldName, level, 2);
					if (!fieldName.equals("_") && basePointsTo.size() == 1) {
						out.insert(k1, valueSet);
					} else {
						out.append(k1, valueSet);
					}

					ObjectInfo k2 = new ObjectInfo(0, baseName+fieldName, level, 2);
					out.append(k2, valueSet);
				}
			}

			Iterator entries = pointerSet.iterator();
			while(entries.hasNext()) {
				ObjectInfo pointer = (ObjectInfo)entries.next();
				out.insert(pointer, valueSet);
			}
		} else if (u instanceof ReturnStmt) {
			PointToSet newSet = new PointToSet();
			returnSet.merge(out, newSet);
			returnSet = newSet;
			Value ret = ((ReturnStmt)u).getOp();
			System.out.println("returnstmt " + ret.toString());
			if (ret instanceof Local) {
				ObjectInfo retObj = new ObjectInfo(0, ret.toString(), level, -3);
				ObjectInfo keyObj = new ObjectInfo(0, ret.toString(), level, 2);
				returnSet.append(retObj, out.getKey(keyObj));
				System.out.println("key Obj ");
				out.dumpKey(keyObj);
				returnSet.dumpPointToSet();
				System.out.println("---\n");
			}
		} else if (u instanceof ReturnVoidStmt) {
			PointToSet newSet = new PointToSet();
			returnSet.merge(out, newSet);
			returnSet = newSet;
		}
		outputSet = out;
		// System.out.println("{{{{{{{{{{{{{{{{{{{{{");
		// System.out.println("Dump In: ");
		// in.dumpPointToSet();
		// System.out.println("Dump Out: ");
		// out.dumpPointToSet();
		// System.out.println("}}}}}}}}}}}}}}}}}}}}}");
		// perform generation (kill set is empty)
		// in.union(genSet, out);
	}

	/**
	 * All paths == Intersection.
	 **/
	protected void merge(PointToSet in1, PointToSet in2, PointToSet out) {
		PointToSet
				inSet1 = (PointToSet) in1,
				inSet2 = (PointToSet) in2,
				outSet = (PointToSet) out;

		inSet1.merge(inSet2, outSet);
	}

	protected void copy(PointToSet source, PointToSet dest) {
		PointToSet
				sourceSet = (PointToSet) source,
				destSet = (PointToSet) dest;

		sourceSet.copy(destSet);
	}
}

// 保存变量的信息，主要由两类，allocID==0表示引用；大于零表示对象，同时表示对象分配点的值。
// 例如 $r3 = new benchmark.objects.A
// 则 $r3的信息为：allocID: 0 object name: $r3 in METHOD main of CLASS default
// 分配的对象的信息：allocID: 1 object name: new benchmark.objects.A in METHOD main of CLASS default
// 表示该对象是在1处分配的
// 定义了一个类来表示信息优点是在域敏感和过程间分析的时候可能会简单一点；但是这样也有缺点，在指向集中可能会重复保存具有相同信息的对象
// 所以保存的时候需要进行一下判断，已经写好了相关的方法。
