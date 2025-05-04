"use client";

import { usePathname } from 'next/navigation';
import ClientNav from '@/components/ClientNav';

export default function LayoutWithNav({ children }) {
  const pathname = usePathname();
  if (pathname === '/login') {
    return children;
  }
  return <ClientNav>{children}</ClientNav>;
} 