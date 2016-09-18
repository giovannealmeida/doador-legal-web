/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.versalius.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author Giovanne
 * @param <T>
 */
public abstract class DAO<T> {
    @PersistenceContext(unitName = "DoadorLegal-webPU")
    private EntityManager em;
    
    private Class<T> entityClass;
    
    @PostConstruct
    public void init() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        this.entityClass = (Class<T>) actualTypeArguments[0];
    }
    
    public void flush(){
        this.em.flush();
    }
    
    public void clearCache(){
        this.em.getEntityManagerFactory().getCache().evictAll();
    }
    
    public void create(T entity) {
        this.em.persist(entity);
    }
    
    public void edit(T entity) {
        this.em.merge(entity);
    }
    
    public void refresh(T entity) {
        this.em.refresh(entity);
    }

    public void remove(T entity) {
        this.em.remove(this.em.merge(entity));
    }
    
    public T find(Object id) {
        return this.em.find(entityClass, id);
    }
    
    public List<T> findAll() {
        CriteriaQuery<T> cq = this.em.getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        return this.em.createQuery(cq).getResultList();
    }
    
    public T findByProperty(String property, Object value) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();
        CriteriaQuery<T> cq = this.em.getCriteriaBuilder().createQuery(entityClass);
        Root<T> from = cq.from(entityClass);
        cq.select(from);
        
        String[] cols = property.split("\\.");
        Path<Object> path = from.get(cols[0]);
        for (int i = 1; i < cols.length; i++) {
            path = path.get(cols[i]);
        }
        
        cq.where(cb.equal(path, value));
        
        T singleResult;
        try {
            singleResult = this.em.createQuery(cq).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            singleResult = null;
        }
        return singleResult;
    }
    
    public T findByProperties(Map<String, Object> properties) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();
        CriteriaQuery<T> cq = this.em.getCriteriaBuilder().createQuery(entityClass);
        Root<T> from = cq.from(entityClass);
        cq.select(from);
        cq.where(cb.and(generateRestrictions(from, cb, properties)));
        
        T singleResult;
        try {
            singleResult = this.em.createQuery(cq).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            singleResult = null;
        }
        return singleResult;
    }
    
    public Predicate[] generateRestrictions(Root<T> root, CriteriaBuilder criteriaBuilder, Map<String, Object> filters) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        if (filters != null) {
            for (String key : filters.keySet()) {
                Path<?> path = root;

                String[] matches = key.split("\\.");
                for (String match : matches) {
                    path = path.get(match);
                }

                Object value = filters.get(key);

                if (value == null) {
                    predicates.add(criteriaBuilder.isNull(path));
                } else if (value.toString().contains("%")) {
                    predicates.add(criteriaBuilder.like(path.as(String.class), value.toString()));
                } else {
                    predicates.add(criteriaBuilder.equal(path, value));
                }
            }
        }
        return predicates.toArray(new Predicate[predicates.size()]);
    }
    
    public T findSingleByQuery(String queryName, String... params) {
        TypedQuery<T> namedQuery = this.em.createNamedQuery(queryName, entityClass);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                namedQuery.setParameter(i + 1, params[i]);
            }
        }
        return namedQuery.getSingleResult();
    }

    public List<T> findByQuery(String queryName, String... params) {
        TypedQuery<T> namedQuery = this.em.createNamedQuery(queryName, entityClass);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                namedQuery.setParameter(i + 1, params[i]);
            }
        }
        return namedQuery.getResultList();
    }

    public int count(Map<String, String> filters) {
        CriteriaBuilder criteruaBuilder = this.em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = criteruaBuilder.createQuery(Long.class);

        Root<T> root = criteria.from(entityClass);
        criteria.select(criteruaBuilder.count(root));

        if (filters != null) {
            Iterator<String> iteratorKeys = filters.keySet().iterator();

            while (iteratorKeys.hasNext()) {
                Path<?> path = root;
                String filterKey = iteratorKeys.next();

                String[] filterSplit = filterKey.split("\\.");
                for (String split : filterSplit) {
                    path = path.get(split);
                }
                criteria.where(criteruaBuilder.equal(path, filters.get(filterKey)));
            }
        }

        Query q = this.em.createQuery(criteria);

        return ((Long) q.getSingleResult()).intValue();
    }
}
