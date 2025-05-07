'use client';

import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';
import { useTranslation } from 'react-i18next';
import React from 'react';
import Button from './Button';
import { X, Menu, LogOut } from 'lucide-react';

export default function Navigation({ onClose }) {
  const { t, i18n } = useTranslation();
  const hasPermission = useAuthStore(state => state.hasPermission);
  const user = useAuthStore(state => state.user);
  const logout = useAuthStore(state => state.logout);
  const router = useRouter();

  // Function to check if menu item should be visible
  const isMenuItemVisible = (menuItem) => {
    if (user?.access === 'SALES') {
      // SALES users can see Home, Stock and Orders
      return menuItem === 'Stock' || menuItem === 'Orders' || menuItem === 'Home';
    }
    return true;
  };

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const menuItems = [
    { name: t('home'), path: '/dashboard' },
    { name: t('productCatalog'), path: '/catalog' },
    { name: t('stock'), path: '/stock' },
    { name: t('orders'), path: '/orders' },
  ];

  const recordItems = [
    { name: t('allRecords'), path: '/records' },
    { name: t('inStockBarcode'), path: '/records/in-stock' },
    { name: t('logs'), path: '/logs' },
  ];

  // Add this function to toggle language
  const toggleLanguage = () => {
    const newLang = i18n.language === 'en' ? 'th' : 'en';
    i18n.changeLanguage(newLang);
  };

  return (
    <div className="fixed md:inset-y-0 md:left-0 md:w-64 w-64 bg-white shadow-lg flex flex-col h-full z-50">
      {/* Mobile Close Button */}
      {onClose && (
        <div className="flex items-center justify-between h-16 px-4 bg-white border-b border-gray-200 md:hidden">
          <span className="text-xl font-semibold text-black">{t('stockManager')}</span>
          <Button
            className="text-gray-500 hover:text-gray-600"
            onClick={onClose}
          >
            <X className="h-6 w-6" />
          </Button>
        </div>
      )}
      {/* Logo (desktop only) */}
      {!onClose && (
        <div className="flex items-center h-16 px-4 bg-white border-b border-gray-200">
          <span className="text-xl font-semibold text-black">{t('stockManager')}</span>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 space-y-6 p-4 overflow-y-auto">
        {/* Main Functions */}
        <div>
          <h2 className="px-4 text-xs font-semibold text-gray-900 uppercase tracking-wider">
            {t('mainFunctions')}
          </h2>
          <div className="mt-2 space-y-1">
            {menuItems.map((item) => (
              isMenuItemVisible(item.name) && (
                <Link 
                  key={item.path}
                  href={item.path} 
                  className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100"
                  onClick={onClose}
                >
                  <span>{item.name}</span>
                </Link>
              )
            ))}
          </div>
        </div>

        {/* Records - Only show for non-SALES users */}
        {user?.access !== 'SALES' && (
          <div>
            <h2 className="px-4 text-xs font-semibold text-gray-900 uppercase tracking-wider">
              {t('records')}
            </h2>
            <div className="mt-2 space-y-1">
              {recordItems.map((item) => (
                <Link 
                  key={item.path}
                  href={item.path} 
                  className="flex items-center px-4 py-2 text-gray-900 hover:bg-gray-100"
                  onClick={onClose}
                >
                  <span>{item.name}</span>
                </Link>
              ))}
            </div>
          </div>
        )}
      </nav>

      {/* Language Switcher */}
      <div className="px-4 pb-2">
        <Button
          onClick={toggleLanguage}
          className="w-full px-4 py-2 mb-2 text-white bg-blue-600 hover:bg-blue-700 rounded-md font-semibold"
        >
          {`Language: ${i18n.language === 'en' ? 'EN' : 'TH'}`}
        </Button>
      </div>

      {/* Logout Button */}
      <div className="p-4 border-t border-gray-200">
        <Button
          className="w-full px-4 py-2 text-left text-red-600 hover:bg-red-50 rounded-md flex items-center"
          onClick={handleLogout}
        >
          <LogOut className="h-6 w-6 mr-2" />
          {t('logout')}
        </Button>
      </div>
    </div>
  );
} 