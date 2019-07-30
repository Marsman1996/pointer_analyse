import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ObjectInfo {
    int allocID ; // 0: variable in test invokestmt; >0: object
    int type; // 2: common local 1: new obj  0:unknown -2: params -1: this
    int level;
    String objectName;
    Map<ObjectInfo, Set<ObjectInfo>> field;
    ObjectInfo(int id, String objectname, int l, int t) {
        allocID = id;
        objectName = objectname;
        level = l;
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

    int getLevel() {
        return level;
    }

    Map<ObjectInfo, Set<ObjectInfo>> getField() {
        return field;
    }

    void dumpObjectInfo() {
        System.out.println("\tallocID: " + allocID + " object name: " + objectName + " level " + level + " type " + type);
        // System.out.println("\tallocID: " + allocID + " object name: " + objectName
        //		+ " in METHOD " + inMethodName + " of CLASS " + inClassName + " and type is " + type);
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
        if (this.allocID == obj.allocID && this.objectName.equals(obj.objectName) && this.level == obj.level && this.type == obj.type) {
            return true;
        }
        return false;
    }

    ObjectInfo deepClone() {
        ObjectInfo newObj = new ObjectInfo(this.allocID, this.objectName, this.level, this.type);
        for (Map.Entry<ObjectInfo, Set<ObjectInfo>> e : field.entrySet()) {
            Set<ObjectInfo> newSet = new HashSet<ObjectInfo>();
            Set<ObjectInfo> oldSet = e.getValue();
            newSet = PointToSet.deepCloneSet(oldSet);
            newObj.field.put(e.getKey().deepClone(), newSet);
        }
        return newObj;
    }
}