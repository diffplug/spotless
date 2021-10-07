resource "aws_instance" "example" {
  count = 2

  ami = "abc123"
  instance_type = "t2.micro"
}
