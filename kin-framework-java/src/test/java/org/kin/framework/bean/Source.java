package org.kin.framework.bean;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2021/9/9
 */
public class Source extends SourceParent {
    private int[] ints = new int[]{1, 2, 3, 4, 5};
    private SourceParent[] sourceParents = new SourceParent[]{new SourceParent(), new SourceParent(), new SourceParent()};
    private List<SourceParent> sourceParentList = Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent());
    private Set<SourceParent> sourceParentSet = new HashSet<>(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent()));
    private Map<Integer, SourceParent> beanMap = new HashMap<>();

    {
        beanMap.put(1, new SourceParent());
        beanMap.put(2, new SourceParent());
        beanMap.put(3, new SourceParent());
        beanMap.put(4, new SourceParent());
        beanMap.put(5, new SourceParent());
    }

    private int[][] intInts = new int[5][];

    {
        intInts[0] = new int[]{1, 2, 3, 4, 5};
        intInts[1] = new int[]{1, 2, 3, 4, 5};
        intInts[2] = new int[]{1, 2, 3, 4, 5};
        intInts[3] = new int[]{1, 2, 3, 4, 5};
        intInts[4] = new int[]{1, 2, 3, 4, 5};
    }

    private SourceParent[][] beanSourceParents = new SourceParent[3][];

    {
        beanSourceParents[0] = new SourceParent[]{new SourceParent(), new SourceParent(), new SourceParent()};
        beanSourceParents[1] = new SourceParent[]{new SourceParent(), new SourceParent(), new SourceParent()};
        beanSourceParents[2] = new SourceParent[]{new SourceParent(), new SourceParent(), new SourceParent()};
    }

    private List<List<SourceParent>> listList = new ArrayList<>();

    {
        listList.add(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent()));
        listList.add(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent()));
        listList.add(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent()));
    }

    private Set<Set<SourceParent>> setSet = new HashSet<>();

    {
        setSet.add(new HashSet<>(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent())));
        setSet.add(new HashSet<>(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent())));
        setSet.add(new HashSet<>(Arrays.asList(new SourceParent(), new SourceParent(), new SourceParent())));
    }

    private Map<Integer, Map<Integer, SourceParent>> mapMap = new HashMap<>();

    {
        mapMap.put(1, Collections.singletonMap(11, new SourceParent()));
        mapMap.put(2, Collections.singletonMap(22, new SourceParent()));
        mapMap.put(3, Collections.singletonMap(33, new SourceParent()));
        mapMap.put(4, Collections.singletonMap(44, new SourceParent()));
        mapMap.put(5, Collections.singletonMap(55, new SourceParent()));
    }

    private List<Map<Integer, SourceParent>> mapList = new ArrayList<>();

    {
        mapList.add(Collections.singletonMap(11, new SourceParent()));
        mapList.add(Collections.singletonMap(22, new SourceParent()));
        mapList.add(Collections.singletonMap(33, new SourceParent()));
    }

    //setter && getter


    public int[] getInts() {
        return ints;
    }

    public void setInts(int[] ints) {
        this.ints = ints;
    }

    public SourceParent[] getSourceParents() {
        return sourceParents;
    }

    public void setSourceParents(SourceParent[] sourceParents) {
        this.sourceParents = sourceParents;
    }

    public List<SourceParent> getSourceParentList() {
        return sourceParentList;
    }

    public void setSourceParentList(List<SourceParent> sourceParentList) {
        this.sourceParentList = sourceParentList;
    }

    public Set<SourceParent> getSourceParentSet() {
        return sourceParentSet;
    }

    public void setSourceParentSet(Set<SourceParent> sourceParentSet) {
        this.sourceParentSet = sourceParentSet;
    }

    public Map<Integer, SourceParent> getBeanMap() {
        return beanMap;
    }

    public void setBeanMap(Map<Integer, SourceParent> beanMap) {
        this.beanMap = beanMap;
    }

    public int[][] getIntInts() {
        return intInts;
    }

    public void setIntInts(int[][] intInts) {
        this.intInts = intInts;
    }

    public SourceParent[][] getBeanSourceParents() {
        return beanSourceParents;
    }

    public void setBeanSourceParents(SourceParent[][] beanSourceParents) {
        this.beanSourceParents = beanSourceParents;
    }

    public List<List<SourceParent>> getListList() {
        return listList;
    }

    public void setListList(List<List<SourceParent>> listList) {
        this.listList = listList;
    }

    public Set<Set<SourceParent>> getSetSet() {
        return setSet;
    }

    public void setSetSet(Set<Set<SourceParent>> setSet) {
        this.setSet = setSet;
    }

    public Map<Integer, Map<Integer, SourceParent>> getMapMap() {
        return mapMap;
    }

    public void setMapMap(Map<Integer, Map<Integer, SourceParent>> mapMap) {
        this.mapMap = mapMap;
    }

    public List<Map<Integer, SourceParent>> getMapList() {
        return mapList;
    }

    public void setMapList(List<Map<Integer, SourceParent>> mapList) {
        this.mapList = mapList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Source)) return false;
        if (!super.equals(o)) return false;
        Source source = (Source) o;

//        System.out.println(Arrays.equals(ints, source.ints));
//        System.out.println(Arrays.equals(sourceParents, source.sourceParents));
//        System.out.println(Objects.equals(sourceParentList, source.sourceParentList));
//        System.out.println(Objects.equals(sourceParentSet, source.sourceParentSet));
//        System.out.println(Objects.equals(beanMap, source.beanMap));
//        System.out.println(Arrays.deepEquals(intInts, source.intInts));
//        System.out.println(Arrays.deepEquals(beanSourceParents, source.beanSourceParents));
//        System.out.println(Objects.equals(listList, source.listList));
//        System.out.println(Objects.equals(setSet, source.setSet));
//        System.out.println(Objects.equals(mapMap, source.mapMap));
//        System.out.println(Objects.equals(mapList, source.mapList));

        return Arrays.equals(ints, source.ints) &&
                Arrays.equals(sourceParents, source.sourceParents) &&
                Objects.equals(sourceParentList, source.sourceParentList) &&
                Objects.equals(sourceParentSet, source.sourceParentSet) &&
                Objects.equals(beanMap, source.beanMap) &&
                Arrays.deepEquals(intInts, source.intInts) &&
                Arrays.deepEquals(beanSourceParents, source.beanSourceParents) &&
                Objects.equals(listList, source.listList) &&
                Objects.equals(setSet, source.setSet) &&
                Objects.equals(mapMap, source.mapMap) &&
                Objects.equals(mapList, source.mapList);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), sourceParentList, sourceParentSet, beanMap, listList, setSet, mapMap, mapList);
        result = 31 * result + Arrays.hashCode(ints);
        result = 31 * result + Arrays.hashCode(sourceParents);
        result = 31 * result + Arrays.hashCode(intInts);
        result = 31 * result + Arrays.hashCode(beanSourceParents);
        return result;
    }

    @Override
    public String toString() {
        return "Source{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", e=" + e +
                ", f=" + f +
                ", g='" + g + '\'' +
                ", h=" + h +
                ", i=" + i +
                ", j=" + j +
                ", k=" + k +
                ", l=" + l +
                ", m=" + m +
                ", list=" + list +
                ", set=" + set +
                ", map=" + map +
                ", ints=" + Arrays.toString(ints) +
                ", beans=" + Arrays.toString(sourceParents) +
                ", beanList=" + sourceParentList +
                ", beanSet=" + sourceParentSet +
                ", beanMap=" + beanMap +
                ", intInts=" + Arrays.toString(intInts) +
                ", beanBeans=" + Arrays.toString(beanSourceParents) +
                ", listList=" + listList +
                ", setSet=" + setSet +
                ", mapMap=" + mapMap +
                ", mapList=" + mapList +
                "} " + super.toString();
    }
}
