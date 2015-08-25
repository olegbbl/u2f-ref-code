// Copyright 2015 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.servlets;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.RegistrationRequest;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@Singleton
public class BeginEnrollServlet extends HttpServlet {
  private final UserService userService = UserServiceFactory.getUserService();
  private final U2FServer u2fServer;

  @Inject
  public BeginEnrollServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    try {
      User user = userService.getCurrentUser();
      boolean singleEnrollment = !Boolean.valueOf(req.getParameter("reregistration"));
      String appId = (req.isSecure() ? "https://" : "http://") + req.getHeader("Host");
      RegistrationRequest registrationRequest =
          u2fServer.getRegistrationRequest(user.getUserId(), appId);
      Collection<RegisteredKey> registeredKeys = ImmutableList.of();
      JsonArray registeredKeysJson = new JsonArray();
      if (singleEnrollment) {
        for (RegisteredKey registeredKey : registeredKeys) {
          JsonObject registeredKeyObj = new JsonObject();
          registeredKeyObj.addProperty("version", registeredKey.getVersion());
          registeredKeyObj.addProperty("keyHandle", registeredKey.getKeyHandle());
          if (registeredKey.getAppId() != null) {
            registeredKeyObj.addProperty("appId", registeredKey.getAppId());
          }
          //TODO: add transports
          registeredKeysJson.add(registeredKeyObj);
        }
      }
      JsonArray registerRequestsArray = new JsonArray();
      JsonObject registerRequest = new JsonObject();
      registerRequest.addProperty("challenge", registrationRequest.getChallenge());
      registerRequest.addProperty("version", registrationRequest.getVersion());
      registerRequestsArray.add(registerRequest);

      JsonObject result = new JsonObject();
      result.addProperty("appId", appId);
      result.add("registeredKeys", registeredKeysJson);
      result.add("registerRequests", registerRequestsArray);
      result.addProperty("sessionId", registrationRequest.getSessionId());
      resp.setContentType("application/json");
      resp.getWriter().println(result.toString());
    } catch (U2FException e) {
      throw new ServletException("couldn't get registration request", e);
    }
  }
}
