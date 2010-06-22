#!/usr/bin/perl
#
# Copyright, Wayfinder Systems AB, 2009
#
# System for exporting strings from Dodona and make string packages for
# jWMMG and Android projects.

require 5.008; # We want the PerlIO system to handle UTF-8

use warnings;
use strict;

use Data::Dumper;
use Encode 'encode';
use File::Copy;
use File::Path 'mkpath';
use File::Spec;
use File::Temp 'tempfile';
use Getopt::Long;
use POSIX 'strftime';
use XML::Simple;

use constant DEBUG_LANGS_CONSTRUCTION => 0;

use open ':encoding(UTF-8)'; # affects only open() calls that don't
                             # set any PerlIO layers themselves

# Requires external programs
#   GNU Wget 1.10.2
#   Info-ZIP 5.51

my $DEBUG = 0;
my %langs = ();

# Warnings/errors (for file or sub process errors we die)
my %keys_not_in_dodona = ();


# add language to %langs
#
# $_[1] is wf lang code
#
# $_[2] parameter hash ref. Default values will be used if they are not
# provided.
sub add_to_langhash($;%) {
    my $FNAME = "add_to_langhash(): ";
    print $FNAME . Dumper(\@_) . "\n" if DEBUG_LANGS_CONSTRUCTION;

    my $code_wf = shift; # prototype makes sure that we cannot call
                         # without this param

    my $params = shift || {}; # this can still be something stupid like
                              # a string. We can only force a hash with \%
                              # prototype, but then the standard idiom
                              # of foo('PL', {a => 1, b => 2}) will not work
                              # since {} already creates a ref to an anon
                              # hash.

    # could have a hash of default paramters and then walk thru
    # %{$params} and check that no unknown (probably misspelled)
    # parameters are present. If we were to export this code, such
    # safety nets would be motivated. For an example, see new() in
    # CSV_PP.pm 1.19

    my %newlang = (code_wf => $code_wf,
                   code_dodona => lc($code_wf),
                   jWMMG_separate_dir => 0,
                   android => 1);

    my $code_dodona = $params->{dodona};
    if (defined($code_dodona)) {
        $newlang{code_dodona} = $code_dodona;
    }


    my $jWMMG_separate_dir = $params->{jWMMG_separate_dir};
    if ($jWMMG_separate_dir) {
        $newlang{jWMMG_separate_dir} = 1;
    }

    my $android = $params->{android};
    if (! defined($android)) {
        # currently, no brandings supported on Android
        if ($code_wf =~ /^[A-Z-]+_/i) {
            $newlang{android} = 0;
        }
    } elsif (! $android) {
            $newlang{android} = 0;
    }

    $langs{lc($code_wf)} = \%newlang;
}


# the standard languages
foreach my $lang qw/CS DA DE EL EN ES FI FR HU IT NL NO
                    PL PT RU SL TR SV/ {
    add_to_langhash($lang, {jWMMG_separate_dir => 1});
    my $dodona_earth = 'wfe.' . lc($lang);
    add_to_langhash($lang . '_earth',
                    {dodona => $dodona_earth,
                     jWMMG_separate_dir => 1,
                     android => 0});
}

# US English - we don't support this on Android currently, since the output
# must go into values-en-rUS/ for which we need an additional parameter in
# the lang data
    add_to_langhash('US', {jWMMG_separate_dir => 1, android => 0});
    add_to_langhash('US_earth', {dodona => 'wfe.us',
                                 jWMMG_separate_dir => 1, android => 0});

# Arabic
add_to_langhash('ar');

# Chinese
add_to_langhash('zh-Hans', {dodona => 'zh', android => 0});
add_to_langhash('zh-Hant', {dodona => 'zht', android => 0});

# # operators - most of these have cancelled contracts but we are not sure
# # if we are still obliged to deliver fixes for serious bugs.
my %oper = (jWMMG_separate_dir => 1, android => 0);
$oper{dodona} = 'etp.cs'; add_to_langhash('CS_eurotelpraha', \%oper);
$oper{dodona} = 'a1n.de'; add_to_langhash('DE_a1at',         \%oper);
$oper{dodona} = 'tnr.en'; add_to_langhash('EN_telenor',      \%oper);
$oper{dodona} = 'etp.en'; add_to_langhash('EN_eurotelpraha', \%oper);
$oper{dodona} = 'tl2.en'; add_to_langhash('EN_tele2',        \%oper);
$oper{dodona} = 'tel.en'; add_to_langhash('EN_telefonica',   \%oper);
$oper{dodona} = 'ain.en'; add_to_langhash('EN_airtel',       \%oper);
$oper{dodona} = 'mai.en'; add_to_langhash('EN_mapmyindia',   \%oper);
$oper{dodona} = 'tel.es'; add_to_langhash('ES_telefonica',   \%oper);
$oper{dodona} = 'sim.sl'; add_to_langhash('SL_Simobil',      \%oper);

print '%langs is now: ' . Dumper(%langs) if DEBUG_LANGS_CONSTRUCTION;


# -------------------------------------------------------------------------
my %conf = (android_dir => '',
            d => 0,
            download => 1,
            jWMMG_dir => '',
            phpurl => 'http://dodona/export/export.php?format=detailed',
            resourceengine => '',
            txtdir => 'txt',
            txtdir_android => 'txt.android',
            );
my @languages_to_process = ();
my @android_languages_to_process = ();


# read args, calls usage if in error (and dies)
sub parse_args_or_die() {
    my $FNAME='parse_args_or_die(): ';

    # initialize defaults
    $conf{txtdir} = 'txt';

    GetOptions(\%conf,
               'android_dir=s',
               'd',
               'download!',
               'phpurl=s',
               'jWMMG_dir=s',
               'txtdir=s',
               'txtdir_android=s'
              ) || usage();

    if ($conf{d}) { $DEBUG=1; }
    if (scalar(@ARGV) == 0) {
        # langs keys are already all lower case, so we can use std sort
        @languages_to_process = sort(keys(%langs));
    } else {
        # langs must be checked for existance. Also we must allow user
        # to use wrong case.
        my @unknowns = ();
        foreach my $arg (@ARGV) {
            my $larg = lc($arg);
            if (exists($langs{$larg})) {
                push @languages_to_process, $larg;
            } else {
                push @unknowns, $arg;
            }
        }
        # print Dumper(\@unknowns);
        # print Dumper(\@languages_to_process);
        if ((scalar @unknowns) > 0) {
            die "Unknown languages: " . join(', ', @unknowns) . "\n";
        }
    }

    foreach my $lang (@languages_to_process) {
        if ($langs{$lang}->{android}) {
            push @android_languages_to_process, $lang;
        }
    }

    my $work = 0;
    if ($conf{jWMMG_dir} ne '') {
        $work++;
        $conf{resourceengine} =
            File::Spec->catfile(($conf{jWMMG_dir},
                                 qw/src wmmg resources/),
                                'ResourceEngine.java');
        print $FNAME, "resourceengine in $conf{resourceengine}\n" if $DEBUG;
    }

    # android stuff
    if ($conf{android_dir} ne '') {
        $work++;
        $conf{strings_xml} =
            File::Spec->catfile(($conf{android_dir},
                                 qw/res values/),
                                'strings.xml');
        print $FNAME, "android strings.xml in $conf{strings_xml}\n" if $DEBUG;

        if ((scalar @android_languages_to_process) == 0) {
            die "No languages in the subset supported by Android were supplied\n";
        }
    }

    if ($work == 0) {
        usage();
    }

    print "$FNAME languages to process: " . join(' ', @languages_to_process)
        . "\n" if $DEBUG;
}


# prints usage information and then dies
sub usage() {
    print <<END;

SYNOPSIS
    export_dodona_make_stringpackages.pl [options] [-android_dir DIRECTORY1]
    [-jWMMG_dir DIRECTORY2] [LANGUAGES]

DESCRIPTION
    Using GNU Wget and Info-Zip's unzip, strings are exported from Dodona.

    If '-android_dir DIRECTORY1' is provided, DIRECTORY1/res/values/strings.xml
    is read and string keys are looked up in the exported data. Then
    DIRECTORY1/res/values-en/strings.xml
    DIRECTORY1/res/values-sv/strings.xml
    ...
    are created with translations for each language.
    Finally values/strings.xml is replaced by values-en/strings.xml

    If [-jWMMG_dir DIRECTORY2] is provided,
    DIRECTORY2/src/wmmg/resources/ResourceEngine.java is read and string keys
    are looked up in the exported data. Then
    DIRECTORY2/Text/EN/strings_EN
    DIRECTORY2/Text/SV/strings_SV
    ...
    are created. Also handles the newer languages, which aren't placed in
    separate directories.

    At least one of -android_dir and -jWMMG_dir must be provided. It
    is supported to supply both.
    If none are provided, no export takes place.

OPTIONS
    -d
        print debugging information

    -nodownload
        use previously downloaded files. Used for tweaking packages on
        old branches.

    -phpurl
        to use other dodona servers for testing.
        Default: $conf{phpurl}

    -txtdir
        directory to read/write Dodona string data for jWMMG.
        Default: $conf{txtdir}

    -txtdir_android
        directory to read/write Dodona string data for jWMMG.
        Default: $conf{txtdir_android}

LANGUAGES
    If you do not provide any languages, all supported languages will
    be processed. Use Wayfinder language codes including any branding
    information.

DIAGNOSTICS
    Normally, exit status is 0 if string packages were exported
    and 1 if packages were exported, but Dodona did not have data for all keys.

    Failure of wget, zip, file operations etc, will yield a non-zero
    exit status.

BUGS
    Due to an error in Dodona's export.php, you can not download just
    one language. So if you provide language arguments you must either
    use -nodownload or provide at least 2 languages.

END
    die;
}


# -------------------------------------------------------------------------

# if the download flag is set, create the download dir. Otherwise
# check that it exists.
sub check_download_dir($) {
    my ($dirname) = @_;

    if ($conf{download}) {
        if (! -d $dirname) {
            # mkpath will die on failure
            mkpath($dirname);
        }
    } else {
        if (! -d $dirname) {
            die "Not downloading from database but txtdir $dirname does not exist";
        }
    }
}


# params:
#
# $dodona_client_id
# $extraction_target_dir
# @dodona_languages 
sub download_and_unpack($$@) {
    my $FNAME = 'download_and_unpack(): ';
    my ($dodona_client_id, $extraction_target_dir, @languages) = @_;
    print $FNAME . Dumper($dodona_client_id, $extraction_target_dir,
                          \@languages) if $DEBUG;

    my ($tmpfh, $tmpfname) = tempfile('export_XXXXXX',
                                     DIR => File::Spec->tmpdir(),
                                     SUFFIX => '.zip',
                                     UNLINK => (! $DEBUG));
    print "$FNAME tmpfname=$tmpfname\n" if $DEBUG;

    my $phpurl = "$conf{phpurl}&client=$dodona_client_id&languages=";
    foreach my $lang (@languages) {
        # print $lang . " " . Dumper($langs{$wflang});
        $phpurl .= "$langs{$lang}->{code_dodona},";
    }
    $phpurl = substr($phpurl, 0, -1); # chop off trailing ',' - dodona
                                      # does not ahndle it

    my @wgetcmd = (qw/wget -O/, $tmpfname,
                   # -r (recursive) is the only way to force wget to
                   # overwrite the target. tempfile() creates the file
                   # (this is part of its security).
                   #
                   # if we add --no-clobber we will never be able to
                   # download
                   $phpurl);
    push @wgetcmd, '-d' if $DEBUG;

    if (system(@wgetcmd)) {
        die "$FNAME system(@wgetcmd) failed: $!";
    }
    my @unzipcmd = ('unzip',
                    '-o', # overwrite without prompting
                    '-j', # junk paths (no path should be in zip but
                          # just to be safe
                    $tmpfname,
                    '-d', $extraction_target_dir,
                   );
    if (system(@unzipcmd)) {
        die "$FNAME system(@unzipcmd) failed: $!";
    }
}


# returns a hash where the keys are the string keys and the values are
# the translations.
#
# params
#   1: directory name
#   2: dodona lang code
sub read_txt_file($$) {
    my ($txtdir, $code_dodona) = @_;
    my %translations = ();

    my $fname = File::Spec->catfile($txtdir, "$code_dodona.txt");
    open(TXT, "<$fname") || die "Could not open translation file $fname";
    while (<TXT>) {
        s/\r?\n//;
        next if ((/^#/) || (/^\s*$/)); # skip # and empty lines
        # No error checking on input from Dodona
        my ($fkey, $fcode_dodona, $ftimestamp, $ftranslation) =
            split(/\t/, $_);

        # remove the quotes surrounding the translation in the file
        $ftranslation =~ s/^\"//;
        $ftranslation =~ s/\"$//;
        # escape sequences
        $ftranslation =~ s/\\n/\n/g;
        $ftranslation =~ s/\\t/\t/g;
        $translations{$fkey} = $ftranslation;
    }
    close(TXT);
    print "read_txt_file() read " . scalar(keys %translations)
        . " translations from $fname\n" if $DEBUG;

    return \%translations;
}


# formats iso8601 date + HH:MM for our commit messages.
#
# this function exists because "%F %R" are not supported arguments in
# win32's strftime
sub cvstime {
    my @ret = ();
    for (@_) {
        my ($sec,$min,$hour,$mday,$month,$year) = localtime($_);
        $year += 1900;
        $month += 1;
        push @ret, sprintf("%04d-%02d-%02d %02d:%02d",
                           $year,$month,$mday,$hour, $min);
    }
    return wantarray ? @ret : $ret[0];
}


# -------------------------------------------------------------------------

# jWMMG functions

# returns a list of string keys (qtn stripped) in the order they are
# defined in ResourceEngine.java (as pointed to by
# $conf{resourceengine}
sub read_resource_engine() {
    my @resengine_keys = ();

    # encoding really correct?
    open(RESENGINE, "<:encoding(iso-8859-1)", $conf{resourceengine})
        || die "Could not open ResourceEngine.java in $conf{resourceengine}";

    my ($key, $line); # use a line variable to facilitate debugging
    while ($line = <RESENGINE>) {
        # print "$line\n";
        if ( ($key) =
             ($line =~ /^\s*public (?:final |static )+short qtn_(\w+)/i)) {
            push @resengine_keys, $key;
        }
    }
    close(RESENGINE);

    print 'Read ' . scalar(@resengine_keys)
        . " string keys from $conf{resourceengine}\n" if $DEBUG;
    return @resengine_keys;
}


sub create_jWMMG_stringpackage(\%\@\%) {
    my $FNAME = 'create_jWMMG_stringpackage(): ';
    # all of these are references...
    my ($langdata, $keylist, $translations) = @_;

    my $code_wf = $langdata->{code_wf};
    my $string_pack_dir = File::Spec->catdir($conf{jWMMG_dir},
                                             'new_resources',
                                             'Text');

    if ($langdata->{jWMMG_separate_dir}) {
        $string_pack_dir = File::Spec->catdir($string_pack_dir,
                                              $code_wf);
    }
    mkpath($string_pack_dir);
    my $string_pack_fname = File::Spec->catfile($string_pack_dir,
                                                "strings_$code_wf");
    open(OUT, ">:raw", $string_pack_fname)
        || die "Could not create string package $string_pack_fname";
    print "$FNAME $code_wf -> $string_pack_fname\n" if $DEBUG;

    foreach my $key (@$keylist) {
        my $translation = $translations->{$key};
        if (! defined($translation)) {
            print "WARNING: $langdata->{code_dodona}: $key not in Dodona.\n" if $DEBUG;
            $keys_not_in_dodona{$key}++;
            $translation = "?$key";
        }

        # length short in network order (upper 8 bits first), then
        # utf-8 bytes
        #
        # This corresponds to Java's DataInput.writeUTF() with the
        # following exceptions:
        #
        # 1. Java: \u0000 is coded in 2-byte format C0 80 which was
        # previously allowed in the Unicode standard.
        #
        # 2. Java: for chars outside U+0000 ... U+FFFF (Basic
        # Multilingual Plane (BMP)), surrogate pairs as in UTF-16 are
        # used. The 2 16-bit values are then encoded to 2 x 3 bytes.
        #
        # 2b - as a consequence - 4-byte UTF-8 sequences are not used.
        #
        # this should not cause any problems fo us currently.
        #
        # n/U* is not supported so we must call encode ourselves
        print OUT pack('n/a*', encode("UTF-8", $translation));
    }
    close(OUT);
}


# -------------------------------------------------------------------------

# Android functions

# returns a list of string keys (qtn stripped) in the order they are
# defined in res/values/strings_xml (as pointed to by
# $conf{strings_xml}
sub read_values_strings_xml() {
    my $FNAME = "read_values_strings_xml():";
    my @string_keys = ();

    my $xml = XMLin($conf{strings_xml},
                    # so that one elem xml still yields an array
                    forcearray => 1,
                    # no default array folding on "name" attr.  thus
                    # order of strings.xml is preserved
                    keyattr => []
                   ); # ref to hash of 2nd elements contained in root.
    # print $FNAME . Dumper($xml) if $DEBUG;
    my $string_elements = $xml->{string}; # ref(array of hash ref)
    my @bad;
    foreach my $elem (@{$string_elements}) {
        if (exists($elem->{name})) {
            my $key = $elem->{name};
            $key =~ s/^qtn_//i;
            push @string_keys, $key;
        } else {
            push @bad, $elem;
        }
    }
    if (scalar @bad) {
        die "ERROR: $FNAME there were unparsable elements in $conf{strings_xml}:\n\t" . Dumper(\@bad);
    }

    print 'Read ' . scalar(@string_keys)
        . " string keys from $conf{strings_xml}\n" if $DEBUG;
    return @string_keys;
}


sub create_android_stringpackage(\%\@\%) {
    my $FNAME = 'create_android_stringpackage(): ';
    # all of these are references...
    my ($langdata, $keylist, $translations) = @_;

    # we might need a separate field in lang data for this in the future.
    my $iso639_1 = lc($langdata->{code_wf});
    my $string_pack_dir = File::Spec->catdir($conf{android_dir},
                                             'res',
                                             "values-$iso639_1");
    mkpath($string_pack_dir);
    my $string_pack_fname = File::Spec->catfile($string_pack_dir,
                                                'strings.xml');

    # no crlf if we run this on win
    open(OUT, ">:raw:utf8", $string_pack_fname)
        || die "Could not create string package $string_pack_fname";
    print "$FNAME $iso639_1 -> $string_pack_fname\n" if $DEBUG;
    print OUT '<?xml version="1.0" encoding="utf-8"?>' . "\n"
        . "<!-- res/values-$iso639_1/strings.xml -->\n\n<resources>\n";
    foreach my $key (@$keylist) {
        my $translation = $translations->{$key};
        if (! defined($translation)) {
            print "WARNING: $langdata->{code_dodona}: $key not in Dodona.\n"
                if $DEBUG;
            $keys_not_in_dodona{$key}++;
            $translation = "\\?$key";
        }
        # the element content will be surrounded with double quotes
        # and then treated as a java string literal, so some chars
        # need to be escaped.
        $translation =~ s/\n/\\n/g;
        $translation =~ s/\t/\\t/g;
        $translation =~ s/\"/\\"/g; # \" is not supposed to be in Dodona

        print OUT "    <string name=\"qtn_$key\">$translation</string>\n";
    }

    print OUT "</resources>\n";
    close(OUT);
}


# =========================================================================
# main
parse_args_or_die();

# we should not require user to make strings for all platforms since
# each Apps-team will work on their own platform and they can easily
# coordinate if a multi-platform release is to be made.


# -------------------------------------------------------------------------
# J2me section
my $jWMMG_export_time = 0;
if ($conf{jWMMG_dir} ne '') {
    # download
    check_download_dir($conf{txtdir});
    if ($conf{download}) {
        $jWMMG_export_time = time();
        download_and_unpack(4, $conf{txtdir}, @languages_to_process);
    }


    my @jWMMG_keylist = read_resource_engine();
    # print "list is now: @jWMMG_keylist\n";

    foreach my $lang (@languages_to_process) {
        my $lang_data = $langs{$lang};

        # read txt file from Dodona
        my $translations = read_txt_file($conf{txtdir},
                                         $lang_data->{code_dodona});

        # dump data to binary string file
        create_jWMMG_stringpackage(%$lang_data,
                                   @jWMMG_keylist,
                                   %$translations);
    }
}


# -------------------------------------------------------------------------
# Android section
my $android_export_time = 0;
if ($conf{android_dir} ne '') {
    check_download_dir($conf{txtdir_android});
    if ($conf{download}) {
        $android_export_time = time();
        download_and_unpack(51, $conf{txtdir_android},
                            @android_languages_to_process);
    }

    my @android_keylist = read_values_strings_xml();
    # print "list is now: " . Dumper(\@android_keylist);

    foreach my $lang (@android_languages_to_process) {
        my $lang_data = $langs{$lang};

        # read txt file from Dodona
        my $translations = read_txt_file($conf{txtdir_android},
                                         $lang_data->{code_dodona});

        # dump data to strings.xml
        create_android_stringpackage(%$lang_data,
                                     @android_keylist,
                                     %$translations);
        if ($lang eq 'en') {
            # as requested by Android team. Enables untranslated strings to
            # fall back to en
            my $src = File::Spec->catfile(($conf{android_dir}, 'res',
                                           'values-en'),
                                         'strings.xml');
            copy($src, $conf{strings_xml})
                || die "Could not copy $src -> $conf{strings_xml}";
        }
    }
}

# -------------------------------------------------------------------------

my @missing_keys = (sort keys %keys_not_in_dodona);
if (scalar @missing_keys) {
    print STDERR "WARNING: The following string keys were not found in Dodona:\n";
    foreach my $key (@missing_keys) {
        print STDERR "\t$key\n";
    }

    exit 1;
}

if ($conf{jWMMG_dir} ne '') {
    print "Exported " . scalar(@languages_to_process) . " languages to"
        . "$conf{jWMMG_dir}\n";
    if ($conf{download}) {
        print "Please:\n\tcvs commit -m 'stringpackage REPLACE_STRING_PACKAGE_NUMBER_HERE exported from Dodona "
            . cvstime($jWMMG_export_time)
            . "' $conf{txtdir}\n";
    }
}
if ($conf{android_dir} ne '') {
    print "Exported " . scalar(@android_languages_to_process) . " languages to"
        . "$conf{android_dir}\n";
    if ($conf{download}) {
        print "Please:\n\tcvs commit -m 'stringpackage exported from Dodona "
            . cvstime($android_export_time)
            . "' $conf{txtdir_android}\n";
    }
}
