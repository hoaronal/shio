package com.viglet.shiohara.widget;

import org.springframework.stereotype.Component;

import com.viglet.shiohara.persistence.model.post.type.ShPostTypeAttr;

@Component
public interface ShWidgetImplementation {
	public String render(ShPostTypeAttr shPostTypeAttr);
}
