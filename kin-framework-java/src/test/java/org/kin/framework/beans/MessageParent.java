package org.kin.framework.beans;

import java.io.Serializable;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2021/9/9
 */
public class MessageParent extends AbstractMessage implements Serializable, Cloneable {
    private static final long serialVersionUID = -16945821338186506L;
    protected byte a;
    protected short b;
    protected int c;
    protected long d;
    protected float e;
    protected double f;
    protected String g;
    protected Byte h;
    protected Short i;
    protected Integer j;
    protected Long k;
    protected Float l;
    protected Double m;
    protected List<Integer> list = new ArrayList<>();
    protected Set<Integer> set = new HashSet<>();
    protected Map<Integer, Long> map = new HashMap<>();

    @Override
    public MessageParent clone() {
        MessageParent copy = new MessageParent();
        copy.a = a;
        copy.b = b;
        copy.c = c;
        copy.d = d;
        copy.e = e;
        copy.f = f;
        copy.g = g;
        copy.h = h;
        copy.i = i;
        copy.j = j;
        copy.k = k;
        copy.l = l;
        copy.m = m;
        copy.list = list;
        copy.set = set;
        copy.map = map;
        return copy;
    }

    //setter && getter
    public byte getA() {
        return a;
    }

    public void setA(byte a) {
        this.a = a;
    }

    public short getB() {
        return b;
    }

    public void setB(short b) {
        this.b = b;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public long getD() {
        return d;
    }

    public void setD(long d) {
        this.d = d;
    }

    public float getE() {
        return e;
    }

    public void setE(float e) {
        this.e = e;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public Byte getH() {
        return h;
    }

    public void setH(Byte h) {
        this.h = h;
    }

    public Short getI() {
        return i;
    }

    public void setI(Short i) {
        this.i = i;
    }

    public Integer getJ() {
        return j;
    }

    public void setJ(Integer j) {
        this.j = j;
    }

    public Long getK() {
        return k;
    }

    public void setK(Long k) {
        this.k = k;
    }

    public Float getL() {
        return l;
    }

    public void setL(Float l) {
        this.l = l;
    }

    public Double getM() {
        return m;
    }

    public void setM(Double m) {
        this.m = m;
    }

    public List<Integer> getList() {
        return list;
    }

    public void setList(List<Integer> list) {
        this.list = list;
    }

    public Set<Integer> getSet() {
        return set;
    }

    public void setSet(Set<Integer> set) {
        this.set = set;
    }

    public Map<Integer, Long> getMap() {
        return map;
    }

    public void setMap(Map<Integer, Long> map) {
        this.map = map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageParent messageParent = (MessageParent) o;
        return a == messageParent.a && b == messageParent.b && c == messageParent.c &&
                d == messageParent.d && Float.compare(messageParent.e, e) == 0 &&
                Double.compare(messageParent.f, f) == 0 && Objects.equals(g, messageParent.g) &&
                Objects.equals(h, messageParent.h) && Objects.equals(i, messageParent.i) &&
                Objects.equals(j, messageParent.j) && Objects.equals(k, messageParent.k) &&
                Objects.equals(l, messageParent.l) && Objects.equals(m, messageParent.m) &&
                Objects.equals(list, messageParent.list) && Objects.equals(set, messageParent.set) &&
                Objects.equals(map, messageParent.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c, d, e, f, g, h, i, j, k, l, m, list, set, map);
    }

    @Override
    public String toString() {
        return "Bean{" +
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
                '}';
    }
}
