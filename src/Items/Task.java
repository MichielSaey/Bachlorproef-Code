package Items;

import jade.content.Concept;

public class Task implements Concept {

	String naam;
//	between 1 and 20
	int size;

	@Override
	public String toString() {
		return "Task{" +
				"naam='" + naam + '\'' +
				", size=" +  size +
				'}';
	}

	public String getNaam() {
		return naam;
	}

	public void setNaam(String naam) {
		this.naam = naam;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		if (size >= 0){
			this.size = size;
		} else {
			this.size = 0;
		}

	}
}
