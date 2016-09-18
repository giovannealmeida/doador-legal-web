/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.versalius.beans;

import br.com.versalius.dao.DAO;
import br.com.versalius.dao.UserDAO;
import br.com.versalius.models.UserModel;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author Giovanne
 */
@ManagedBean
@SessionScoped
public class UserBean {

    private UserModel user;

    @EJB
    private UserDAO userDAO;
    /**
     * Creates a new instance of UserBean
     */
    public UserBean() {
        user = new UserModel();
    }

    public String saveUser(){
        if(user != null){
            userDAO.create(user);
            return "success";
        }
        return "failure";
    }

    public UserModel getUser() {
        return user;
    }    
}
