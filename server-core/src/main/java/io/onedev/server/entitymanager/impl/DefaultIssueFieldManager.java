package io.onedev.server.entitymanager.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import io.onedev.server.entitymanager.IssueFieldManager;
import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueField;
import io.onedev.server.persistence.annotation.Sessional;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.AbstractEntityManager;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.util.inputspec.InputSpec;

@Singleton
public class DefaultIssueFieldManager extends AbstractEntityManager<IssueField> implements IssueFieldManager {

	@Inject
	public DefaultIssueFieldManager(Dao dao) {
		super(dao);
	}

	@Transactional
	@Override
	public void saveFields(Issue issue) {
		Collection<Long> ids = new HashSet<>();
		for (IssueField entity: issue.getFields()) {
			if (entity.getId() != null)
				ids.add(entity.getId());
		}
		if (!ids.isEmpty()) {
			Query query = getSession().createQuery("delete from IssueField where issue = :issue and id not in (:ids)");
			query.setParameter("issue", issue);
			query.setParameter("ids", ids);
			query.executeUpdate();
		} else {
			Query query = getSession().createQuery("delete from IssueField where issue = :issue");
			query.setParameter("issue", issue);
			query.executeUpdate();
		}
		
		for (IssueField entity: issue.getFields()) {
			if (entity.isNew())
				save(entity);
		}
	}

	@Transactional
	@Override
	public void onRenameGroup(String oldName, String newName) {
		Query query = getSession().createQuery("update IssueField set value=:newName where type=:groupChoice and value=:oldName");
		query.setParameter("groupChoice", InputSpec.GROUP);
		query.setParameter("oldName", oldName);
		query.setParameter("newName", newName);
		query.executeUpdate();
	}

	@Transactional
	@Override
	public void onRenameUser(String oldName, String newName) {
		Query query = getSession().createQuery("update IssueField set value=:newName where type=:userChoice and value=:oldName");
		query.setParameter("userChoice", InputSpec.USER);
		query.setParameter("oldName", oldName);
		query.setParameter("newName", newName);
		query.executeUpdate();
	}

	@Sessional
	@Override
	public void populateFields(List<Issue> issues) {
		CriteriaBuilder builder = getSession().getCriteriaBuilder();
		CriteriaQuery<IssueField> query = builder.createQuery(IssueField.class);
		
		Root<IssueField> root = query.from(IssueField.class);
		query.select(root);
		root.join("issue");
		
		Expression<String> issueExpr = root.get("issue");
		query.where(issueExpr.in(issues));
		
		for (Issue issue: issues)
			issue.setFields(new ArrayList<>());
		
		for (IssueField field: getSession().createQuery(query).getResultList())
			field.getIssue().getFields().add(field);
	}
	
}