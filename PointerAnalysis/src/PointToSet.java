import java.util.*;

//
public class PointToSet
{
    Map<ObjectInfo, Set<ObjectInfo>> pointSet;


    PointToSet() {
        pointSet = new HashMap<ObjectInfo, Set<ObjectInfo>>();
        pointSet.clear();
    }

    public int hashCode() {
        return pointSet.hashCode();
    }

    public boolean equals(Object pSet) {
        PointToSet pTSet = (PointToSet)pSet;
        Map<ObjectInfo, Set<ObjectInfo>> ps = pTSet.getPointSet();
        if (ps.size() == pointSet.size() && ps.size() == 0) {
            return true;
        }
        if (ps.size() != pointSet.size()) {
            return false;
        }
        for(ObjectInfo pointer : this.pointSet.keySet()) {
            if (pTSet.containsKey(pointer)) {
                Set<ObjectInfo> valueSet = getKey(pointer);
                Set<ObjectInfo> valueSet2 = pTSet.getKey(pointer);
                if (setEquals(valueSet, valueSet2) == false) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }



    static boolean setEquals(Set<ObjectInfo> valueSet1, Set<ObjectInfo> valueSet2) {
        if (valueSet1.size() != valueSet2.size()) {
            return false;
        }
        Iterator entries = valueSet1.iterator();
        while(entries.hasNext()){
            ObjectInfo value = (ObjectInfo)entries.next();
            if (setContains(valueSet2, value) == false) {
                return false;
            }
        }
        return true;
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
                pSet.insert(key.deepClone(), PointToSet.deepCloneSet(entry.getValue()));
            }
        }
    }

    Set<ObjectInfo> funcRetCopy(PointToSet pSet) {
        Set<ObjectInfo> retSet = new HashSet<ObjectInfo>();
        for (Map.Entry<ObjectInfo, Set<ObjectInfo>> entry : this.getPointSet().entrySet()) {
            ObjectInfo key = entry.getKey();
            if (key.getType() >= 0) {
                pSet.insert(key, entry.getValue());
            } else if (key.getType() == -3) { // Return value.
                retSet = PointToSet.deepCloneSet(entry.getValue());
            }
        }
        return retSet;
    }

    void insert(ObjectInfo key, Set<ObjectInfo> value) {
        ObjectInfo pointer = aliasKey(key);
        pointSet.remove(pointer);
        pointSet.put(pointer.deepClone(), PointToSet.deepCloneSet(value));
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
        // System.out.println("\n");
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
