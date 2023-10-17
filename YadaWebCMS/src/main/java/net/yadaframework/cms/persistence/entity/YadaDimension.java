package net.yadaframework.cms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Entity for a product dimension
 *
 */
@Embeddable
public class YadaDimension {

	@Column(length=16)
	protected String size; // M, L, 42, ...

	protected Float width;

	protected Float height;

	protected Float depth;

	protected Float length;

	protected Float diameter;

	protected Float radius;

	protected Float weight;

	protected Integer elements;

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public Float getWidth() {
		return width;
	}

	public void setWidth(Float width) {
		this.width = width;
	}

	public Float getHeight() {
		return height;
	}

	public void setHeight(Float height) {
		this.height = height;
	}

	public Float getDepth() {
		return depth;
	}

	public void setDepth(Float depth) {
		this.depth = depth;
	}

	public Float getDiameter() {
		return diameter;
	}

	public void setDiameter(Float diameter) {
		this.diameter = diameter;
	}

	public Float getWeight() {
		return weight;
	}

	public void setWeight(Float weight) {
		this.weight = weight;
	}

	public Integer getElements() {
		return elements;
	}

	public void setElements(Integer elements) {
		this.elements = elements;
	}

	public Float getLength() {
		return length;
	}

	public void setLength(Float length) {
		this.length = length;
	}

	public Float getRadius() {
		return radius;
	}

	public void setRadius(Float radius) {
		this.radius = radius;
	}

}
