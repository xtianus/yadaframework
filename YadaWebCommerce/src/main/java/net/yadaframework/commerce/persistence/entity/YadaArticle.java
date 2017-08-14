package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;

import net.yadaframework.components.YadaUtil;

@Entity
public class YadaArticle implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@Column(length=32)
	protected String code;
	
	@ElementCollection
	@Column(length=1024)
	@MapKeyColumn(name="locale", length=32) // th_TH_TH_#u-nu-thai
	protected Map<Locale, String> color;
	
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


}
