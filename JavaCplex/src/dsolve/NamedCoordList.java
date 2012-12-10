package dsolve;

import cgl.imr.base.SerializationException;
import cgl.imr.base.Value;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/18/12
 * Time: 6:21 PM
 */

public class NamedCoordList implements Cloneable, Value {
	private List<NamedCoord> list = new ArrayList<NamedCoord>();

	// auxiliary storage for fast acces
	private Map<String, NamedCoord>  namesMap = new HashMap<String, NamedCoord>();
	private Map<Integer, NamedCoord> indicesMap = new HashMap<Integer, NamedCoord>();

	public NamedCoordList() { }

	public NamedCoordList( int size ) {
		list = new ArrayList<NamedCoord>( size );

		namesMap = new HashMap<String, NamedCoord>( size );
		indicesMap = new HashMap<Integer, NamedCoord>( size );
	}

	public NamedCoordList rebuild() {
		Collections.sort( list );

		// rebuild indices
		for ( int i=0; i<list.size(); i++ ) {
			NamedCoord obj = list.get( i );

			namesMap.put( obj.name, obj );
			indicesMap.put( i, obj );
		}

		return this;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		NamedCoordList myClone = ( NamedCoordList ) super.clone();
		List<NamedCoord> cloneList = new ArrayList<NamedCoord>( list.size() );

		for ( NamedCoord coord : list ) {
			cloneList.add( coord );
		}
		myClone.list = cloneList;

		return myClone;
	}

	public void add ( NamedCoord coord ) {
		this.list.add( coord );
	}

	public int size() {
		return list.size();
	}

	public List<NamedCoord> getList() {
		return list;
	}

	public NamedCoord get( int index ) {
		return indicesMap.get( index );
	}

	public NamedCoord get( String name ) {
		return namesMap.get( name );
	}

	public boolean has( String name ) {
		return namesMap.containsKey( name );
	}

	private String convertToString() {
		String stringList = "";

		if ( list.size() == 0 ) { return stringList; }

		for ( NamedCoord namedCoord : list ) {
			stringList += String.format( "%s:%f,", namedCoord.name, namedCoord.val );
		}
		return stringList.substring( 0, stringList.length()-1 );
	}

	@Override
	public String toString() {
		return convertToString();
	}

	@Override
	public void fromBytes( byte[] bytes ) throws SerializationException {
		String[] coordComp;

		list.clear();

		String stringList = new String( bytes );
		String [] coordPairs = stringList.split( "," );
		for ( String pair : coordPairs ) {
			coordComp = pair.split( ":" );
			list.add( new NamedCoord( coordComp[0], Double.parseDouble( coordComp[1] ) ) );
		}
		this.rebuild();
	}

	@Override
	public byte[] getBytes() throws SerializationException {
		return convertToString().getBytes();
	}

	public boolean isEmpty() {
		return list.size() == 0;
	}
}
