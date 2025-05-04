'use client';

import { useState } from 'react';
import Navigation from './Navigation';

export default function ClientNav({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      {/* Mobile Top Bar */}
      <div className="md:hidden flex items-center justify-between bg-white shadow px-4 py-3 sticky top-0 z-30">
        <button
          aria-label="Open sidebar"
          onClick={() => setSidebarOpen(true)}
          className="text-gray-700 focus:outline-none"
        >
          <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <span className="font-bold text-lg text-gray-900">Stock Manager</span>
        <div className="w-6" /> {/* Spacer for symmetry */}
      </div>

      {/* Sidebar */}
      <div>
        {/* Desktop sidebar */}
        <div className="hidden md:block">
          <Navigation />
        </div>
        {/* Mobile sidebar overlay */}
        {sidebarOpen && (
          <div className="fixed inset-0 z-40 flex">
            <div className="fixed inset-0 bg-black bg-opacity-30" onClick={() => setSidebarOpen(false)}></div>
            <div className="relative w-64 bg-white shadow-lg h-full z-50 animate-slide-in-left">
              <Navigation onClose={() => setSidebarOpen(false)} />
            </div>
          </div>
        )}
      </div>

      {/* Main content */}
      <div className="flex-1 md:pl-64">
        <main className="p-4 sm:p-8">
          {children}
        </main>
      </div>
    </div>
  );
} 