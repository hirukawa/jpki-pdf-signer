package net.osdn.jpki.ui;

import javafx.scene.control.ListCell;

public interface ListCellUpdateItemListener<T> {
	
	public void updateItem(ListCell<T> cell, T item, boolean empty);

}
