package Slave_Terminal;

use strict;

use IO::Pty;
use POSIX;
use Fcntl;

@Slave_Terminal::ISA = qw( IO::Pty );

# Spawn a new process inside a slave terminal
sub spawn
{
  my $class = shift;

  my $slaveHandle;
  my $nameOfPty;

  # Allocate a new pseudo-terminal
  my $self = IO::Pty->new();
  bless($self, $class);

  my $cmd = join(' ', @_);  # command to run inside the slave pty
  $nameOfPty = $self->IO::Pty::ttyname();
  die "$class: Could not assign a pty" unless $self->IO::Pty::ttyname();
  $self->IO::Pty::autoflush();

  ${*$self}{slavePID} = fork;

  unless (defined (${*$self}{slavePID}))
  {
    warn "Cannot fork: $!";
    return undef;
  }

  unless (${*$self}{slavePID})
  {
    # Child Process: Create a new 'session' -- dissociate from controlling terminal.

    POSIX::setsid() || warn "$class: POSIX::setsid() failed\r\nProblem: $!\r\n";

    $slaveHandle = $self->IO::Pty::slave();  # Create slave handle.
    $nameOfPty   = $slaveHandle->ttyname();

    close($self);
    close STDIN; close STDOUT; close STDERR;

    open(STDIN,  "<&" . $slaveHandle->fileno()) || die "Couldn't reopen STDIN as "  . $nameOfPty . " for reading, $!\r\n";
    open(STDOUT, ">&" . $slaveHandle->fileno()) || die "Couldn't reopen STDOUT as " . $nameOfPty . " for writing, $!\r\n";
    open(STDERR, ">&" . fileno(STDOUT)) || die "Couldn't redirect STDERR, $!\r\n";

    exec ($cmd);
  }

  return $self;
} # end sub spawn


# Read characters from the slave terminal's STDOUT/STDERR
sub slave_read
{
  my $self = shift; 
  my ($rmask, $nfound, $nread);
  my $buf = '';
  my $shell_output = '';

  READLOOP:
  while (1)
  {
    $rmask = '';
    vec($rmask, $self->fileno(), 1) = 1;
    ($nfound) = select($rmask, undef, undef, 0);
    last READLOOP unless $nfound;

    $nread = sysread($self, $buf, 2048);
    $nread = 0 unless defined ($nread);

    if ($nread) { $shell_output .= $buf; }
    else        { last READLOOP; }
  }
  # End READLOOP

  return $shell_output;
} # end sub slave_read


# Return the process ID of the spawned process
sub slave_pid
{
  my $self = shift;
  return ${*$self}{slavePID};
}

