import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import polyglot.ast.New;
import soot.Local;


// 这个文件没什么用，之后删掉。
class AssignConstraint {
	String from, to;
	AssignConstraint(String from, String to) {
		this.from = from;
		this.to = to;
	}
}

class NewConstraint {
	String to;
	int allocId;
	NewConstraint(int allocId, String to) {
		this.allocId = allocId;
		this.to = to;
	}
}

public class Anderson {
	private List<AssignConstraint> assignConstraintList = new ArrayList<AssignConstraint>();
	private List<NewConstraint> newConstraintList = new ArrayList<NewConstraint>();
	Map<String, TreeSet<Integer>> pts = new HashMap<String, TreeSet<Integer>>();
	void addAssignConstraint(String from, String to) {
		assignConstraintList.add(new AssignConstraint(from, to));
	}
	void addNewConstraint(int alloc, String to) {
		newConstraintList.add(new NewConstraint(alloc, to));		
	}
	void showAssignConstraintList() {
		System.out.println("assignConstraintList");
		for (AssignConstraint ac : assignConstraintList) {
			System.out.println(ac.from + " -> " + ac.to);
		}
	}
	void showNewConstraintList() {
		System.out.println("newConstraintList");
		for (NewConstraint nc : newConstraintList) {
			System.out.println(nc.allocId + " -> " +nc.to);
		}
	}
	void showPTS() {
		System.out.println("PTS");
		System.out.println(pts);
	}
	void run() {
		for (NewConstraint nc : newConstraintList) {
			if (!pts.containsKey(nc.to)) {
				pts.put(nc.to, new TreeSet<Integer>());
			}
			pts.get(nc.to).add(nc.allocId);
		}
		for (boolean flag = true; flag; ) {
			flag = false;
			for (AssignConstraint ac : assignConstraintList) {
				if (!pts.containsKey(ac.from)) {
					continue;
				}	
				if (!pts.containsKey(ac.to)) {
					pts.put(ac.to, new TreeSet<Integer>());
				}
				if (pts.get(ac.to).addAll(pts.get(ac.from))) {
					flag = true;
				}
			}
		}
	}
	TreeSet<Integer> getPointsToSet(String local) {
		if (!pts.containsKey(local)) {
			return new TreeSet<Integer>();
		}
		return pts.get(local);
	}
	
}
