package net.yadaframework.persistence.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;


@Entity
@Table(
	    uniqueConstraints = @UniqueConstraint(columnNames={"name", "clauseVersion"})
	)
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaClause implements Serializable {
	private static final long serialVersionUID = 1L;

	// For optimistic locking
	@Version
	private long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
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

	public long getVersion() {
		return version;
	}
	
	
}
