package it.polito.tdp.extflightdelays.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	
	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	private ExtFlightDelaysDAO dao;
	private Map<Integer, Airport> idMap; //identityMap per i vertici del grafo
	private Map<Airport, Airport> visita; //altrimenti la classe interna non la vede
	
	public Model() {
		dao= new ExtFlightDelaysDAO();
		idMap= new HashMap<Integer,Airport>();
		dao.loadAllAirports(idMap);
	}
	
	public void creaGrafo(int x) {
		grafo= new SimpleWeightedGraph(DefaultWeightedEdge.class);
		//non posso farlo perchè i vertici sono un sotto insieme di aereoporti che rispecchiano il vincolo di avere almeno x airlines
		//Graphs.addAllVertices(grafo, idMap.values());
		//questo sotto insieme me lo faccio dare dal dao
		Graphs.addAllVertices(grafo, dao.getVertici(idMap, x));
		
		//aggiungo gli archi
		for(Rotta r:dao.getRotte(idMap)) {
			//ho tutte le rotte del db ma noi non sappiamo se sono tutti nel grafo, devo controllarlo
			if(grafo.containsVertex(r.getA1()) && grafo.containsVertex(r.getA2())) {
				//controllo se c'è già un arco per questi aereoporti, se c'è già vuol dire che ho già incontrato questa coppia di aereoporti e sto consiederando la coppia inversa
				DefaultWeightedEdge e= this.grafo.getEdge(r.getA1(), r.getA2());
				if(e==null)
					Graphs.addEdgeWithVertices(grafo,r.getA1(), r.getA2(), r.getN());
				else { //perchè devo tenere conto sia di andata che ritorno per quello aggiungo
					double pesoVecchio=this.grafo.getEdgeWeight(e);
					double pesoNuovo=pesoVecchio+r.getN();
					grafo.setEdgeWeight(e, pesoNuovo);
				}
					
			}
		}
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2){
		List<Airport> percorso= new LinkedList<>();
		//creo iteratore che ha metodo come has next che mi permette di visitare il grafo passo per passo
		BreadthFirstIterator <Airport, DefaultWeightedEdge> it= new BreadthFirstIterator<>(grafo,a1);
		//visito il grafo
		visita = new HashMap<>(); //qui salvo l'albero così posso dire che aereporto 2 l'ho scoperto da aereoorto1
		visita.put(a1, null); //a1 è il punto di partenza (radice albero) ed è associato a null perchè il percorso lo devo andare a scoprire)
		//mi permette di registrare degli eventi in modo tale da poter salvare l'albero di visita, partendo da questo recupero il percorso
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {				
			}
			//quando attraverso arco salvo la sorgente e la destinazione così da avere l'albero di visita che salvo in una mappa
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				Airport airport1= grafo.getEdgeSource(e.getEdge());
				Airport airport2= grafo.getEdgeTarget(e.getEdge());
				//essendo il grafo non orientato
				if(visita.containsKey(airport1) && !visita.containsKey(airport2)) //a1 avevo già visitato in partenza, ed è quindi il padre di a2
					visita.put(airport2, airport1);
				else if(visita.containsKey(airport2) && !visita.containsKey(airport1)) //a2 già lo conoscevo quindi a2 è il padre di a1
					visita.put(airport1, airport2);
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {	
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
			}
			
		});
		while(it.hasNext()) {
			it.next();  //restituisce un aereoporto, prima quelli di livello 1 poi livello 2 etc
			//voglio trovare un percorso, non mi basta la visita, quindi prendo l'labero dal traversalListener
		}
		if(!visita.containsKey(a1) || !visita.containsKey(a2)) //controllo il caso in cui non ci sia il percorso
			return null;
		//ora percorro l'albero e vedo se c'è un percorso
		percorso.add(a2);
		Airport step=a2;
		while(visita.get(step)!=null) { //percorro l'albero
			step=visita.get(step);
			percorso.add(step);	//percorso trovato		
		}
		return percorso;
	}

	public Set<Airport> getVertici() {
		if(grafo != null)
			return grafo.vertexSet();
		
		return null;
	}
	
	public int getNVertici() {
		if(grafo != null)
			return grafo.vertexSet().size();
		
		return 0;
	}
	
	public int getNArchi() {
		if(grafo != null)
			return grafo.edgeSet().size();
		
		return 0;
	}




}
