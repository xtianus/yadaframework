package net.yadaframework.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


@Entity
@Table(
	    uniqueConstraints = @UniqueConstraint(columnNames={"name", "clauseVersion"})
	)
public class YadaClause implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@Column(nullable=false, length=32)
	private String name;
	
	// Non ho usato "versione" perchè potrebbe essere una parola chiave e dare fastidio
	private int clauseVersion;
	
	@Lob
	private String content;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getClauseVersion() {
		return clauseVersion;
	}

	public void setClauseVersion(int version) {
		this.clauseVersion = version;
	}
	
	
}
