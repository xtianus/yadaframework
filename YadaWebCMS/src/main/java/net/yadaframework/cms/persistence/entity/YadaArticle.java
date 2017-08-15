package net.yadaframework.cms.persistence.entity;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MapKeyColumn;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaMoneyConverter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED) // So that subclasses will have their own neat table
public class YadaArticle implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@Column(length=32)
	protected String code;
	
	@ElementCollection
	@Column(length=32)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> color;
	
	@Embedded
	protected YadaDimension dimension;
	
	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney unitPrice;
	
	/**
	 * 
	 * @param locale
	 * @return
	 */
	public String getColor(Locale locale) {
		return YadaUtil.getLocalValue(color, locale);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Map<Locale, String> getColor() {
		return color;
	}

	public void setColor(Map<Locale, String> description) {
		this.color = description;
	}

	public YadaDimension getDimension() {
		return dimension;
	}

	public void setDimension(YadaDimension dimension) {
		this.dimension = dimension;
	}

	public YadaMoney getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(YadaMoney unitPrice) {
		this.unitPrice = unitPrice;
	}
}
