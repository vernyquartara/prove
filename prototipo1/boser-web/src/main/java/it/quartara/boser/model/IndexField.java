package it.quartara.boser.model;

public enum IndexField {

	URL,
	TITLE,
	CONTENT;

	@Override
	public String toString() {
		String fieldName = super.toString();
		return fieldName.toLowerCase();
	}

	
}
