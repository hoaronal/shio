package com.viglet.shiohara.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import com.viglet.shiohara.persistence.model.object.ShObject;
import com.viglet.shiohara.persistence.model.post.type.ShPostType;
import com.viglet.shiohara.persistence.model.post.type.ShPostTypeAttr;
import com.viglet.shiohara.persistence.model.widget.ShWidget;
import com.viglet.shiohara.persistence.repository.object.ShObjectRepository;
import com.viglet.shiohara.persistence.repository.post.type.ShPostTypeRepository;
import com.viglet.shiohara.persistence.repository.widget.ShWidgetRepository;
import com.viglet.shiohara.widget.ShSystemWidget;
import com.viglet.shiohara.widget.ShWidgetImplementation;

@Component
public class ShFormComponent {
	@Autowired
	private ShPostTypeRepository shPostTypeRepository;
	@Resource
	private ApplicationContext applicationContext;
	@Autowired
	private SpringTemplateEngine templateEngine;
	@Autowired
	private ShObjectRepository shObjectRepository;
	
	public String byPostType(String shPostTypeName, String shObjectId, HttpServletRequest request)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		final Context ctx = new Context();

		ShObject shObject = shObjectRepository.findById(shObjectId).get();
		ShPostType shPostType = shPostTypeRepository.findByName(shPostTypeName);
		CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

		List<String> fields = new ArrayList<String>();

		List<ShPostTypeAttr> postTypeAttrByOrdinal = new ArrayList<ShPostTypeAttr>(shPostType.getShPostTypeAttrs());

		Collections.sort(postTypeAttrByOrdinal, new Comparator<ShPostTypeAttr>() {

			public int compare(ShPostTypeAttr o1, ShPostTypeAttr o2) {
				return o1.getOrdinal() - o2.getOrdinal();
			}
		});

		for (ShPostTypeAttr shPostTypeAttr : postTypeAttrByOrdinal) {
			String className = shPostTypeAttr.getShWidget().getClassName();
			ShWidgetImplementation object = (ShWidgetImplementation) Class.forName(className).newInstance();
			applicationContext.getAutowireCapableBeanFactory().autowireBean(object);
			fields.add(object.render(shPostTypeAttr, shObject));
		}

		String token = null;
		if (csrf != null) {
			token = csrf.getToken();
		}

		ctx.setVariable("token", token);
		ctx.setVariable("shPostType", shPostType);
		ctx.setVariable("shPostTypeAttrs", shPostType.getShPostTypeAttrs());
		ctx.setVariable("fields", fields);

		return templateEngine.process("form", ctx);
	}
}
