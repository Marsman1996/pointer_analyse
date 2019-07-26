import org.jboss.util.Null;
import org.omg.CORBA.OBJ_ADAPTER;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.awt.*;
import java.util.*;
import java.util.List;

class MyForwardFlow extends ForwardFlowAnalysis
{
	// ArraySparseSet<PointToSet> emptySet = new ArraySparseSet<PointToSet>();
	// Set<ObjectInfo> objectSet = new HashSet<ObjectInfo>();
	PointToSet inputSet = new PointToSet();
	PointToSet outputSet = new PointToSet();
	PointToSet returnSet = new PointToSet();
	UnitGraph graph;
	Integer allocID = -1;
	TreeMap<Integer, ObjectInfo> queries = new TreeMap<Integer, ObjectInfo>();
	String methodName;
	String className;

	MyForwardFlow(UnitGraph graph, PointToSet in, String name, String cName) {
		super(graph);
		this.graph = graph;
		this.methodName = name;
		this.className = cName;
		in.dumpPointToSet();
		in.copy(this.inputSet);
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
	protected Object newInitialFlow() {
		return inputSet.deepClone();
	}

	/**
	 * IN(Start) is the empty set
	 **/
	protected Object entryInitialFlow() {
		return inputSet.deepClone();
	}


	Set<ObjectInfo> handleInvoke(InvokeExpr ie, PointToSet in) {
		SootMethod sm = ie.getMethod();
		if (sm.getDeclaringClass().isJavaLibraryClass()) {
			return new HashSet<ObjectInfo>();
		}
		PointToSet initSet = new PointToSet();
		in.funcCallCopy(initSet);

		UnitGraph graph = new ExceptionalUnitGraph(sm.retrieveActiveBody());
		if (ie instanceof SpecialInvokeExpr) {
			String baseName = ((Local) ((SpecialInvokeExpr)ie).getBase()).getName();
			System.out.println("Base name is " + baseName);
			ObjectInfo baseObj = new ObjectInfo(0, baseName, className, methodName, 0);
			ObjectInfo thisObj = new ObjectInfo(0, "this", className, methodName, -1);
			initSet.insert(thisObj, in.getKey(baseObj));
		}

		List<Value> args = ie.getArgs();
		for (int i = 0; i < args.size(); i++) {
			Value arg = args.get(i);
			if (arg instanceof Local) {
				System.out.println("Arg Name is " + arg.toString());
				ObjectInfo argObj = new ObjectInfo(0, arg.toString(), className, methodName, 0);
				ObjectInfo formalArgObj = new ObjectInfo(0, Integer.toString(i), className, methodName, -2);
				initSet.insert(formalArgObj, in.getKey(argObj));
			}
		}
		MyForwardFlow flowAnalysis = new MyForwardFlow(graph, initSet, sm.getName(), sm.getDeclaringClass().toString());
		Set<ObjectInfo> retSet = new HashSet<ObjectInfo>();
		retSet = flowAnalysis.returnSet.funcRetCopy(outputSet);
		mergeQueries(flowAnalysis.queries);
		return retSet;
		// TODO: Copy return value.
	}

	protected void flowThrough(Object inValue, Object unit, Object outValue) {
		PointToSet
				in = (PointToSet) inValue,
				out = (PointToSet) outValue;
		in.copy(out);
		Unit u = (Unit) unit;

		System.out.println("[" + className + ":" + methodName + "]: " + unit);
		if (u instanceof InvokeStmt) { // TODO: 主要在这里补充，估计还需要补充200行左右
			InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
			if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void alloc(int)>")) {
				// System.out.println("Invoke*****alloc\n");
				allocID = ((IntConstant)ie.getArgs().get(0)).value;
				//System.out.println("allocID = " + allocID);
			} else if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>")) {
				//System.out.println("Invoke*****test\n");
				Value v = ie.getArgs().get(1);
				int id = ((IntConstant)ie.getArgs().get(0)).value;
				ObjectInfo o = new ObjectInfo(0, v.toString(), className, methodName, 0);
				queries.put(id, o);
				//System.out.println("test invoke" + ": " + id + "," + v.toString());
			} else { // TODO: 处理函数调用。如果不是上述两种情况，则需要进入将参数拷贝给方法的形参，进入到函数内部分析
				handleInvoke(((InvokeStmt) u).getInvokeExpr(), out);
			}
		}
		if (u instanceof DefinitionStmt) {
			DefinitionStmt dsu = (DefinitionStmt)u;
			Value rightOp = dsu.getRightOp();
			Value leftOp = dsu.getLeftOp();

			Set<ObjectInfo> pointerSet = new HashSet<ObjectInfo>();
			Set<ObjectInfo> valueSet = new HashSet<ObjectInfo>();

			if (rightOp instanceof  NewExpr) {
				ObjectInfo rightObj = new ObjectInfo(allocID, rightOp.toString(), className, methodName, 1);
				valueSet.add(rightObj);
			} else if (rightOp instanceof Local) {
				ObjectInfo rightObj = new ObjectInfo(0, rightOp.toString(), className, methodName, 2);
				valueSet = out.getKey(rightObj);
			} else if (rightOp instanceof InstanceFieldRef) {
				InstanceFieldRef instsRefOp = (InstanceFieldRef) rightOp;
				ObjectInfo baseObj = new ObjectInfo(0, instsRefOp.getBase().toString(), className, methodName, 2);
				Set<ObjectInfo> basePointSet = out.getKey(baseObj);

				Iterator entries = basePointSet.iterator();
				while(entries.hasNext()){
					ObjectInfo value = (ObjectInfo)entries.next();
					String objName = value.objectName + "." + instsRefOp.getField().toString();
					ObjectInfo fieldObj = new ObjectInfo(value.getAllocID(), objName, value.inClassName, value.inMethodName, 2);
					Set<ObjectInfo> fieldPointSet = out.getKey(fieldObj);
					valueSet.addAll(PointToSet.deepCloneSet(fieldPointSet));  // Deep clone?
				}
			} else if (rightOp instanceof ThisRef) {
				ObjectInfo rightObj = new ObjectInfo(0, "this", className, methodName, 0);
				valueSet = out.getKey(rightObj);
			} else if (rightOp instanceof ParameterRef) {
				String rightName = Integer.toString(((ParameterRef)rightOp).getIndex());
				ObjectInfo rightObj = new ObjectInfo(0, rightName, className, methodName, -2);
				valueSet = out.getKey(rightObj);
			} else if (rightOp instanceof InvokeExpr) {
				valueSet = handleInvoke((InvokeExpr)rightOp, out);
			} else if (rightOp instanceof ArrayRef) {
				System.out.println("DEBUG: ArrayRef called");

				ArrayRef arr = (ArrayRef) rightOp;
				String baseName = ((Local) arr.getBase()).getName();
				String fieldName = "_";
				if (arr.getIndex() instanceof IntConstant) {
					fieldName = Integer.toString(((IntConstant) arr.getIndex()).value);
				}

				ObjectInfo rightObj = new ObjectInfo(0, baseName, "default", "main", 0);
				Set<ObjectInfo> basePointsTo = in.getKey(rightObj);

				for (ObjectInfo pto: basePointsTo) {
					ObjectInfo k1 = new ObjectInfo(pto.getAllocID(), baseName, "_", pto.getInMethodName(), 0);
					valueSet.add(k1);
					if (!fieldName.equals("_")) {
						//固定位置
						ObjectInfo k2 = new ObjectInfo(pto.getAllocID(), baseName, fieldName, pto.getInMethodName(), 0);
						valueSet.add(k2);
					}
				}
			}

			if (leftOp instanceof Local) {
				ObjectInfo leftObj = new ObjectInfo(0, leftOp.toString(), className, methodName, 2);
				pointerSet.add(leftObj);
			} else if (leftOp instanceof InstanceFieldRef) {
				InstanceFieldRef instsRefOp = (InstanceFieldRef) leftOp;
				// System.out.println(instsRefOp.getField().toString() + "***" + instsRefOp.getBase().toString());
				ObjectInfo baseObj = new ObjectInfo(0, instsRefOp.getBase().toString(), className, methodName, 2);
				Set<ObjectInfo> basePointSet = out.getKey(baseObj);

				Iterator entries = basePointSet.iterator();
				while(entries.hasNext()) {
					ObjectInfo value = (ObjectInfo)entries.next();
					String objName = value.objectName + "." + instsRefOp.getField().toString();
					ObjectInfo fieldObj = new ObjectInfo(value.getAllocID(), objName, value.inClassName, value.inMethodName, 2);
					pointerSet.add(fieldObj);
				}
			} else if (leftOp instanceof ArrayRef) {
				ArrayRef arr = (ArrayRef) leftOp;
				String baseName = ((Local) arr.getBase()).getName();
				String fieldName = "_";
				if (arr.getIndex() instanceof IntConstant) {
					fieldName = Integer.toString(((IntConstant) arr.getIndex()).value);
				}

				ObjectInfo leftIFObj = new ObjectInfo(0, baseName, "default", "main", 0);
				Set<ObjectInfo> basePointsTo = in.getKey(leftIFObj);

				for (ObjectInfo pto: basePointsTo) {
					ObjectInfo k1 = new ObjectInfo(pto.getAllocID(), baseName, "_", pto.getInMethodName(), 0);
					if (!fieldName.equals("_") && basePointsTo.size() == 1) {
						out.insert(k1, valueSet);
					} else {
						out.append(k1, valueSet);
					}

					ObjectInfo k2 = new ObjectInfo(0, baseName, fieldName, "main", 0);
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
			if (ret instanceof Local) {
				ObjectInfo retObj = new ObjectInfo(0, ret.toString(), className, methodName, -3);
				returnSet.append(retObj, out.getKey(retObj));
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
	protected void merge(Object in1, Object in2, Object out) {
		PointToSet
				inSet1 = (PointToSet) in1,
				inSet2 = (PointToSet) in2,
				outSet = (PointToSet) out;

		inSet1.merge(inSet2, outSet);
	}

	protected void copy(Object source, Object dest) {
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

class ObjectInfo {
	int allocID ; // 0: variable in test invokestmt; >0: object
	int type; // 2: common local 1: new obj  0:unknown -2: params -1: this
	String objectName;
	String inClassName;
	String inMethodName;
	Map<ObjectInfo, Set<ObjectInfo>> field;
	ObjectInfo(int id, String objectname, String inclassname, String inmethodname, int t) {
		allocID = id;
		objectName = objectname;
		inClassName = inclassname;
		inMethodName = inmethodname;
		type = t;
		field = new HashMap<ObjectInfo, Set<ObjectInfo>>();
	}

	int getAllocID() {
		return allocID;
	}
	int getType() {return type;}

	String getBaseClassName() {
		return objectName;
	}

	String getInMethodName() {
		return inMethodName;
	}

	Map<ObjectInfo, Set<ObjectInfo>> getField() {
		return field;
	}

	void dumpObjectInfo() {
		System.out.println("\tallocID: " + allocID + " object name: " + objectName
				+ " in METHOD " + inMethodName + " of CLASS " + inClassName + " and type is " + type);
		if (!field.isEmpty()) {
			for (Map.Entry<ObjectInfo, Set<ObjectInfo>> e : field.entrySet()) {
				System.out.println("Field\n");
				e.getKey().dumpObjectInfo();
				System.out.println("\t:\t");
			}
		}
	}
	boolean equal(ObjectInfo obj) {
		if (obj == null) {
			return false;
		}
		if (this.allocID == obj.allocID && this.objectName.equals(obj.objectName)) {
			return true;
		}
		return false;
	}

	ObjectInfo deepClone() {
		ObjectInfo newObj = new ObjectInfo(this.allocID, this.objectName, this.inClassName, this.inMethodName, this.type);
		for (Map.Entry<ObjectInfo, Set<ObjectInfo>> e : field.entrySet()) {
			Set<ObjectInfo> newSet = new HashSet<ObjectInfo>();
			Set<ObjectInfo> oldSet = e.getValue();
			newSet = PointToSet.deepCloneSet(oldSet);
			newObj.field.put(e.getKey().deepClone(), newSet);
		}
		return newObj;
	}
}
//
class PointToSet
{
	Map<ObjectInfo, Set<ObjectInfo>> pointSet;

	PointToSet() {
		pointSet = new HashMap<ObjectInfo, Set<ObjectInfo>>();
		pointSet.clear();
	}

	Map<ObjectInfo, Set<ObjectInfo>> getPointSet() {
		return pointSet;
	}

	boolean containsKey(ObjectInfo key) {
		for (ObjectInfo pointer : pointSet.keySet()) {
			if (pointer.equal(key)) {
				return true;
			}
		}
		return false;
	}

	Set<ObjectInfo> getKey(ObjectInfo key) {
		ObjectInfo pointer = aliasKey(key);
		return pointSet.get(pointer);
	}

	ObjectInfo aliasKey(ObjectInfo key) {
		for (ObjectInfo pointer : pointSet.keySet()) {
			if (pointer.equal(key)) {
				return pointer;
			}
		}
		Set<ObjectInfo> valueSet = new HashSet<ObjectInfo>();
		valueSet.clear();
		pointSet.put(key, valueSet);
		return key;
	}

	PointToSet deepClone() {
		PointToSet newSet = new PointToSet();
		for(ObjectInfo pointer : this.pointSet.keySet()) {
			ObjectInfo newKey = pointer.deepClone();
			Set<ObjectInfo> newValueSet = deepCloneSet(getKey(pointer));
			newSet.insert(newKey, newValueSet);
		}
		return newSet;
	}

	static Set<ObjectInfo> deepCloneSet(Set<ObjectInfo> oldSet) {
		Set<ObjectInfo> newValueSet = new HashSet<ObjectInfo>();
		newValueSet.clear();
		Iterator entries = oldSet.iterator();
		while(entries.hasNext()){
			ObjectInfo value = (ObjectInfo)entries.next();
			newValueSet.add(value.deepClone());
		}
		return newValueSet;
	}

	void assign(PointToSet pSet) {
		pSet.pointSet = this.pointSet;
	}

	void copy(PointToSet pSet) {
		PointToSet newSet = this.deepClone();
		pSet.pointSet = newSet.pointSet;
	}

	void funcCallCopy(PointToSet pSet) {
		PointToSet newSet = this.deepClone();
		for (Map.Entry<ObjectInfo, Set<ObjectInfo>> entry : this.getPointSet().entrySet()) {
			ObjectInfo key = entry.getKey();
			if (key.getType() >= 0) {
				pSet.insert(key, entry.getValue());
			}
		}
	}

	Set<ObjectInfo> funcRetCopy(PointToSet pSet) {
		PointToSet newSet = this.deepClone();
		Set<ObjectInfo> retSet = new HashSet<ObjectInfo>();
		for (Map.Entry<ObjectInfo, Set<ObjectInfo>> entry : this.getPointSet().entrySet()) {
			ObjectInfo key = entry.getKey();
			if (key.getType() >= 0) {
				pSet.insert(key, entry.getValue());
			} else if (key.getType() == -3) { // Return value.
				retSet = entry.getValue();
			}
		}
		return retSet;
	}

	void insert(ObjectInfo key, Set<ObjectInfo> value) {
		ObjectInfo pointer = aliasKey(key);
		pointSet.remove(pointer);
		pointSet.put(pointer, value);
	}

	void clearKey(ObjectInfo key) {
		ObjectInfo pointer = aliasKey(key);
		pointSet.remove(pointer);
	}

	void append(ObjectInfo key, Set<ObjectInfo> valueSet) {
		Set<ObjectInfo> oldValueSet = getKey(key);
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo value = (ObjectInfo)entries.next();
			if (!setContains(oldValueSet, value)) {
				oldValueSet.add(value.deepClone());
			}
		}
	}

	void merge(PointToSet pSet, PointToSet newSet) {
		this.copy(newSet);
		for(ObjectInfo pointer : pSet.getPointSet().keySet()) {
			ObjectInfo newKey = pointer.deepClone();
			Set<ObjectInfo> newValueSet = deepCloneSet(pSet.getKey(pointer));
			newSet.append(newKey, newValueSet);
		}
	}

	static boolean setContains(Set<ObjectInfo> valueSet, ObjectInfo value) {
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo e = (ObjectInfo)entries.next();
			if (e.equal(value)) {
				return true;
			}
		}
		return false;
	}

	static void setInsert(Set<ObjectInfo> valueSet, ObjectInfo value) {
		if (!setContains(valueSet, value)) {
			valueSet.add(value);
		}
	}


	static ObjectInfo setValueAlias(Set<ObjectInfo> valueSet, ObjectInfo value) {
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo e = (ObjectInfo)entries.next();
			if (e.equal(value)) {
				return e;
			}
		}
		valueSet.add(value);
		return value;
	}

	void dumpPointToSet() {
		for (ObjectInfo pointer : pointSet.keySet()) {
			dumpKey(pointer);
		}
	}

	void dumpKey(ObjectInfo key) {
		Set<ObjectInfo> valueSet = getKey(key);
		if (valueSet.isEmpty()) {
			return;
		}
		System.out.println("Pointer: ");
		key.dumpObjectInfo();
		dumpSet(valueSet);
		System.out.println("\n");
	}

	void dumpSet(Set<ObjectInfo> valueSet) {
		System.out.println("Object Set: ");
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo value = (ObjectInfo)entries.next();
			value.dumpObjectInfo();
		}
	}
}
