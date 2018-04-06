package com.viglet.shiohara.persistence.model.post;

import javax.persistence.*;

import org.hibernate.annotations.Fetch;
//import org.hibernate.search.annotations.Field;
//import org.hibernate.search.annotations.FieldBridge;
//import org.hibernate.search.annotations.Indexed;
//import org.hibernate.search.annotations.Store;
import org.springframework.beans.factory.annotation.Configurable;

import com.viglet.shiohara.persistence.model.folder.ShFolder;
import com.viglet.shiohara.persistence.model.object.ShObject;
import com.viglet.shiohara.persistence.model.post.type.ScanResultBridge;
import com.viglet.shiohara.persistence.model.post.type.ShPostType;

import java.util.Date;
import java.util.List;

/**
 * The persistent class for the ShPost database table.
 * 
 */
@Configurable(preConstruction = true)
//@Indexed
@Entity
@NamedQuery(name = "ShPost.findAll", query = "SELECT s FROM ShPost s")
public class ShPost extends ShObject {
	private static final long serialVersionUID = 1L;
	
	@Temporal(TemporalType.TIMESTAMP)
//	@Field(store = Store.NO)
	private Date date;

//	@Field(store = Store.YES)
	private String summary;

//	@Field(store = Store.YES)
	private String title;

	// bi-directional many-to-one association to ShPostType
	@ManyToOne
	@JoinColumn(name = "post_type_id")
//	@Field(store = Store.NO)
//	@FieldBridge(impl = ScanResultBridge.class) 
	private ShPostType shPostType;

	// bi-directional many-to-one association to ShFolder
	@ManyToOne
	@JoinColumn(name = "folder_id")
//	@Field(store = Store.NO)
	//@FieldBridge(impl = ScanResultBridge.class) 
	private ShFolder shFolder;

	// bi-directional many-to-one association to ShPostAttr
//	@Field(store = Store.NO)
	@OneToMany(mappedBy = "shPost", cascade = CascadeType.ALL)
	//@OneToMany(fetch = FetchType.LAZY, mappedBy = "shPost", cascade = CascadeType.ALL)
	//@Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
//	@FieldBridge(impl = ScanResultBridge.class) 
	private List<ShPostAttr> shPostAttrs;

	public ShPost() {
	}

	
	public Date getDate() {
		return this.date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getSummary() {
		return this.summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ShPostType getShPostType() {
		return this.shPostType;
	}

	public void setShPostType(ShPostType shPostType) {
		this.shPostType = shPostType;
	}

	public List<ShPostAttr> getShPostAttrs() {
		return this.shPostAttrs;
	}

	public void setShPostAttrs(List<ShPostAttr> shPostAttrs) {
		this.shPostAttrs = shPostAttrs;
	}

	public ShPostAttr addShPostAttr(ShPostAttr shPostAttr) {
		getShPostAttrs().add(shPostAttr);
		shPostAttr.setShPost(this);

		return shPostAttr;
	}

	public ShPostAttr removeShPostAttr(ShPostAttr shPostAttr) {
		getShPostAttrs().remove(shPostAttr);
		shPostAttr.setShPost(null);

		return shPostAttr;
	}

	public ShFolder getShFolder() {
		return shFolder;
	}

	public void setShFolder(ShFolder shFolder) {
		this.shFolder = shFolder;
	}

}
