import React from 'react';
import { Routes, Route } from 'react-router-dom';
import LoginPage from './pages/login';
import HomePage from './pages/home';
import RegisterPage from './pages/register'
import UserInfoSetting from "./pages/UserInfoSetting";
import RegisterWizard from "./pages/register_avatar";
import MQTTConfigPage from "./pages/MQTTConfigSetting";
import SetTopicPage from "./pages/setTopic"
import CatDashboard from "./pages/CatDashBoard";

function App() {
  return (
      <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/home" element={<HomePage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/register_avatar" element={<RegisterWizard />} />
          <Route path="/userInfo" element={<UserInfoSetting />} />
          <Route path="/mqtt-config" element={<MQTTConfigPage />} />
          <Route path="/subscribe_topic" element={<SetTopicPage />} />
          <Route path="/cat_dashboard" element={<CatDashboard />} />
          <Route path="*" element={<LoginPage />} />
      </Routes>
  );
}

export default App;
