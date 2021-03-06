Name: exif
Summary: A utility to display EXIF headers from JPEG files
Version: 0.6.22
Release: 1
Source: https://downloads.sourceforge.net/project/libexif/exif/%{version}/exif-%{version}.tar.bz2
Url: https://libexif.github.io/
Group: Applications/Multimedia
License: LGPL
BuildRoot: %{_tmppath}/%{name}-%{version}-root
BuildRequires: libexif-devel
Requires: libexif

%description
'exif' is a small command-line utility to show EXIF information hidden
in JPEG files. It was written to demonstrate the power of libexif.

%prep
%setup

%build
%configure
make

%install
rm -rf $RPM_BUILD_ROOT
%makeinstall

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc ChangeLog README NEWS
%{_bindir}/*
%{_mandir}/man*/*
%{_datadir}/locale/*/LC_MESSAGES/*.mo
