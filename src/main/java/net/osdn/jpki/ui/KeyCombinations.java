package net.osdn.jpki.ui;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class KeyCombinations extends KeyCombination {

	private Object obj = new Object();
	private KeyCombination[] keyCombinations;
	
	public KeyCombinations(KeyCombination... keyCombinations) {
		this.keyCombinations = keyCombinations;
	}
	
	@Override
	public boolean match(KeyEvent event) {
		if(keyCombinations == null) {
			return false;
		}
		for(KeyCombination keyCombination : keyCombinations) {
			boolean match = keyCombination.match(event);
			if(match) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return obj.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj == this);
	}
}
