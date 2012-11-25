package dsolve;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 11/3/12
 * Time: 4:20 PM
 */

public class NamedCoord implements Comparable, Cloneable {
    public String name;
    public double val;

	public NamedCoord() {
		name = "";
		val = 0.0;
	}

    public NamedCoord( String name, double value ) {
        this.name = name;
        this.val = value;
    }

    public void copy( NamedCoord other ) {
        this.name = other.name;
        this.val = other.val;
    }

	@Override
	public int compareTo( Object o ) {
		NamedCoord other = (NamedCoord)o;

		int comp = this.name.compareTo( other.name );
		if ( comp != 0 ) { return comp; }

		return Double.compare( this.val, other.val );
	}

	@Override
	public boolean equals( Object obj ) {
		NamedCoord other = (NamedCoord) obj;
		return this.compareTo( other ) == 0;
	}
}
