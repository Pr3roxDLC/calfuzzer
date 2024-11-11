package javato.activetesting.syncpd.util;


import java.util.Objects;

public class Quaternary<A, B, C, D> {
	public A first;
	public B second;
	public C third;
	public D fourth;
	
 	public Quaternary(A a, B b, C c, D d){
		this.first = a;
		this.second = b;
		this.third = c;
		this.fourth = d;
	}
	
 	@Override
 	public boolean equals(Object o){
       if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		Quaternary<?, ?, ?, ?> quaternary = (Quaternary<?, ?, ?, ?>) o;

		if (!first.equals(quaternary.first))
			return false;
		if (!second.equals(quaternary.second))
			return false;
		if (!third.equals(quaternary.third))
			return false;
		return fourth.equals(quaternary.fourth);
    }

    public int hashCode(){
      return Objects.hash(first, second, third, fourth);
    }
    
    public String toString(){
    	String str = "<";
    	str += first;
    	str += ", ";
    	str += second;
    	str += ", ";
    	str += third;
    	str += ", ";
    	str += fourth;
    	str += ">";
    	return str;
    }
}
