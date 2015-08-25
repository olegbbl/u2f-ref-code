// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.servlets;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.SignRequest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@Singleton
public class BeginSignServlet extends HttpServlet {
    
    private final UserService userService =  UserServiceFactory.getUserService();
    private final U2FServer u2fServer;
    
    @Inject
    public BeginSignServlet(U2FServer u2fServer) {
        this.u2fServer = u2fServer;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException, ServletException {
      try {
      User user = userService.getCurrentUser();
      String appId = (req.isSecure() ? "https://" : "http://") + req.getHeader("Host");
      SignRequest signRequest = u2fServer.getSignRequest(user.getUserId(), appId);
      JsonArray registeredKeysJson = new JsonArray();
      for(RegisteredKey registeredKey : signRequest.getRegisteredKeys()) {
        JsonObject registeredKeyJson = new JsonObject();
        registeredKeyJson.addProperty("version", registeredKey.getVersion());
        registeredKeyJson.addProperty("keyHandle", registeredKey.getKeyHandle());
        if(registeredKey.getAppId() != null) {
          registeredKeyJson.addProperty("appId", registeredKey.getAppId());
        }
        //TODO: add transports
        registeredKeysJson.add(registeredKeyJson);
      }
      JsonObject result = new JsonObject();
      result.addProperty("appId", signRequest.getAppId());
      result.addProperty("challenge", signRequest.getChallenge());
      result.add("registeredKeys", registeredKeysJson);
      result.addProperty("sessionId", signRequest.getSessionId());
      resp.setContentType("application/json");
      resp.getWriter().println(result.toString());
      } catch (U2FException e) {
        throw new ServletException("couldn't get registration request", e);
      }
    }
}
